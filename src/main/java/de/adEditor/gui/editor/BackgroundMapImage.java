package de.adEditor.gui.editor;

import de.adEditor.AppConfig;
import de.adEditor.ApplicationContextProvider;
import de.adEditor.gui.graph.GNode;
import de.adEditor.helper.IconHelper;
import de.adEditor.service.HttpClientService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static de.autoDrive.NetworkServer.rest.MapTileInfo.DIMENSIONS;
import static de.autoDrive.NetworkServer.rest.MapTileInfo.TILE_SIZE;

public class BackgroundMapImage {

    private static Logger LOG = LoggerFactory.getLogger(BackgroundMapImage.class);

    private Rectangle rectangle;
    private int currentZoomLevel = -1;
    private static final double[] scale = {1, 2.5, 5, 7.5, 10, 12.5, 15};
    private MapInfo mapInfo;

    private CacheManager cacheManager = ApplicationContextProvider.getContext().getBean(CacheManager.class);
    private HttpClientService httpClientService = ApplicationContextProvider.getContext().getBean(HttpClientService.class);

    private Cache<String, Image> cache1 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L1, String.class, Image.class);
    private Cache<String, byte[]> cache2 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L2, String.class, byte[].class);
    private Cache<String, byte[]> cache3 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L3, String.class, byte[].class);

    private Map<String, Long> requests = new HashMap<>();
    private final Semaphore available = new Semaphore(1);
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private MapPanel mapPanel;

    public BackgroundMapImage(MapInfo mapInfo, int width, int height, MapPanel mapPanel) {
        this.mapInfo = mapInfo;
        this.rectangle = new Rectangle(0, 0, width, height);
        this.mapPanel = mapPanel;
        zoom(0);
    }


    public void draw(Graphics2D g) {

        if (rectangle != null) {
            for (int x = (rectangle.x / TILE_SIZE) * TILE_SIZE, ix = 0; x < rectangle.x + rectangle.width; x += TILE_SIZE, ix += TILE_SIZE) {
                for (int y = (rectangle.y / TILE_SIZE) * TILE_SIZE, iy = 0; y < rectangle.y + rectangle.height; y += TILE_SIZE, iy += TILE_SIZE) {

                    int kx = rectangle.x % TILE_SIZE;
                    int ky = rectangle.y % TILE_SIZE;
                    BufferedImage tile = getTile(currentZoomLevel, x, y);
                    g.drawImage(tile, ix - kx, iy - ky, tile.getWidth(), tile.getHeight(), null);

                    g.setColor(Color.RED);
                    g.drawRect(ix - kx, iy - ky, tile.getWidth(), tile.getHeight());
                    g.drawString("x: " + x + ", y: " + y, ix - kx + 5, iy - ky + 15);
                }
            }
        }
    }

    private BufferedImage getTile(int zoomLevel, int x, int y) {
        String k = toCacheKey(zoomLevel, x, y);
        Image tile = cache1.get(k);
        if (tile != null) {
            return (BufferedImage) tile;
        } else {
            LOG.info("cache1 empty: {}", k);

            byte[] data = cache2.get(k);
            if (data!=null) {
                BufferedImage img = toImage(data);
                cache1.put(k, img);
                return img;
            } else {
                data = cache3.get(k);
                if (data!=null) {
                    BufferedImage img = toImage(data);
                    cache1.put(k, img);
                    return img;
                } else {

                    LOG.info("mapDB empty: {}", k);

                    if (!requests.containsKey(k)) {
                        httpClientService.getMap("FELSBRUNN", zoomLevel, x, y, event -> {
                            if (event.isPresent()) {
                                byte[] imgData = event.get();
                                BufferedImage image = toImage(imgData);
                                cache1.put(k, image);
                                cache2.put(k, imgData);
                                requests.remove(k);
                                mapPanel.repaint();
                            } else {
                                Runnable runnableTask = () -> {
                                    fillCache(zoomLevel, k);
                                    mapPanel.repaint();
                                };
                                executor.submit(runnableTask);
                            }
                        });
                        requests.put(k, System.currentTimeMillis());
                    }
                }
            }
        }

        LOG.info("nothing found: {}", k);
        return new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    public boolean zoom(int level) {
        long start = System.currentTimeMillis();
        int newZoomLevel;
        if (currentZoomLevel + level >= scale.length) {
            newZoomLevel = scale.length - 1;
        } else if (currentZoomLevel + level < 0) {
            newZoomLevel = 0;
        } else {
            newZoomLevel = currentZoomLevel + level;
        }

        if (currentZoomLevel!=newZoomLevel) {
            currentZoomLevel = newZoomLevel;
            LOG.info("zoom currentZoomLevel: {} in {}ms", currentZoomLevel, System.currentTimeMillis() - start);
            return true;
        } else {
            LOG.info("zoom not changed currentZoomLevel: {} in {}ms", currentZoomLevel, System.currentTimeMillis() - start);
            return false;
        }
    }

    private void fillCache(int currentZoomLevel, String key) {
        LOG.info("fillCache level: {}", currentZoomLevel);
        try {
            available.acquire();
            if (!cache3.containsKey(key)) {
                int w = DIMENSIONS[currentZoomLevel];
                int h = DIMENSIONS[currentZoomLevel];

                BufferedImage originalImage = IconHelper.loadImage("/mapImages/" + mapInfo.getMap() + ".png");
                BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, (int) w, (int) h);
                LinkedList<Pair<Integer, Integer>> workQueue = new LinkedList<>();

                for (int x = 0; x < w; x += TILE_SIZE) {
                    for (int y = 0; y < h; y += TILE_SIZE) {
                        workQueue.add(new ImmutablePair<>(x, y));
                    }
                }

                workQueue.parallelStream().forEach(p -> {
                    Integer x = p.getLeft();
                    Integer y = p.getRight();
                    try {
                        String k = toCacheKey(currentZoomLevel, x, y);
                        BufferedImage cutoutImage = scaledImage.getSubimage(x, y, x + TILE_SIZE <= w ? TILE_SIZE:w - x, y + TILE_SIZE <= h ? TILE_SIZE:h - y);
                        byte[] byteArray = toByteArrayAutoClosable(cutoutImage);
                        cache3.put(k, byteArray);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
            }
        } catch (Throwable t) {
           LOG.error(t.getMessage(), t);
        }
        finally {
            available.release();
        }

        LOG.info("end fillCache level: {}", currentZoomLevel);
    }

    private byte[] toByteArrayAutoClosable(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    private BufferedImage toImage(byte[] imageInByte) {

        try (InputStream in = new ByteArrayInputStream(imageInByte)) {
            return ImageIO.read(in);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void move(int x, int y, int dx, int dy) {
        if (rectangle != null) {
            rectangle.x += x;
            rectangle.y += y;
            rectangle.width = dx;
            rectangle.height = dy;

            if (rectangle.x < 0) {
                rectangle.x = 0;
            }

            if (rectangle.y < 0) {
                rectangle.y = 0;
            }

            Integer dimension = mapInfo.getDimensions();

            if (rectangle.x + rectangle.width > dimension*getScaleFactor()) {
                rectangle.x = (int) ((dimension*getScaleFactor()) - rectangle.width);
            }

            if (rectangle.y + rectangle.height > dimension*getScaleFactor()) {
                rectangle.y = (int) ((dimension*getScaleFactor()) - rectangle.height);
            }

            if (rectangle.width > dimension*getScaleFactor()) {
                rectangle.width = (int) (dimension*getScaleFactor());
            }

            if (rectangle.height > dimension*getScaleFactor()) {
                rectangle.height = (int) (dimension*getScaleFactor());
            }
        }
    }

    public Rectangle getScaledRectangle() {
        return new Rectangle((int)(rectangle.x* getScaleFactor()), (int)(rectangle.y* getScaleFactor()), (int)(rectangle.width* getScaleFactor()), (int)(rectangle.height* getScaleFactor()));
    }

    public double getScaleFactor() {
        return scale[currentZoomLevel];
    }

    public int getZoomLevel() {
        return currentZoomLevel;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public GNode screenPosToWorldVertex(int x, int y) {
        Point2D worldPos = screenPosToWorldPos (new Point (x, y));
        return new GNode (worldPos);
    }

    public Point2D screenPosToWorldPos(Point point) {
        Rectangle viewPort = getRectangle();
        double scaleFactor = getScaleFactor();
        double worldPosX = (point.x  + viewPort.x) / scaleFactor;
        double worldPosY = (point.y  + viewPort.y) / scaleFactor;
        return new Point2D.Double(worldPosX-1024, worldPosY-1024);
    }


    public Point worldVertexToScreenPos(GNode gNode) {
        return worldPosToScreenPos(gNode.getPoint2D());
    }

    public Point worldPosToScreenPos(Point2D p) {
        Rectangle viewPort = getRectangle();
        double scaleFactor = getScaleFactor();
        double screenPosX = ((p.getX()+1024)*scaleFactor) - viewPort.x;
        double screenPosY = ((p.getY()+1024)*scaleFactor) - viewPort.y;
        return new Point((int) screenPosX, (int) screenPosY);
    }

    private String toCacheKey (int zoomLevel, int x, int y) {
        StringBuilder sb = new StringBuilder();
        sb.append(zoomLevel).append("-");
        sb.append(x).append("-");
        sb.append(y);
        return sb.toString();
    }
}
