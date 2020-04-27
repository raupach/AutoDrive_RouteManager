package de.adEditor.mapper;

import de.adEditor.gui.editor.MapMarker;
import de.adEditor.gui.editor.MapNode;
import de.adEditor.gui.editor.RoadMap;
import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import de.autoDrive.NetworkServer.rest.dto_v1.MarkerDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RouteEditorMapper {

    private static Logger LOG = LoggerFactory.getLogger(RouteEditorMapper.class);

    public RoadMap toRoadMap(WaypointsResponseDto waypointsResponseDto) {
        RoadMap roadMap = new RoadMap();
//        for (int i = 0; i < waypointsResponseDto.getWaypoints().size(); i++) {
//            WaypointDto waypointDto = waypointsResponseDto.getWaypoints().get(i);
//            MapNode mapNode = toMapNode(i, waypointDto, waypointsResponseDto.getWaypoints());
//            roadMap.addMapNode(mapNode);
//        }
//
//        roadMap.setMapMarkers(waypointsResponseDto.getMarkers().stream().map(m -> toMapMarker(m, waypointsResponseDto.getWaypoints().get(m.getWaypointIndex()))).collect(Collectors.toList()));
//
//        roadMap.setGraph(xx(waypointsResponseDto));

        return roadMap;
    }


    private MapMarker toMapMarker(MarkerDto markerDto, WaypointDto waypointDto) {
        return new MapMarker( new MapNode(0, waypointDto.getX(), waypointDto.getY(), waypointDto.getZ()), markerDto.getName(), markerDto.getGroup());
    }

    private MapNode toMapNode(int i, WaypointDto waypointDto, List<WaypointDto> waypoints) {
        MapNode mn = new MapNode(i + 1, waypointDto.getX(), waypointDto.getY(), waypointDto.getZ());
        mn.setOutgoing(waypointDto.getOut().stream().map(j -> new MapNode(0, waypoints.get(j-1).getX(), waypoints.get(j-1).getY(), waypoints.get(j-1).getZ())).collect(Collectors.toList()));

        return mn;
    }


    private Graph<GNode, GEdge> xx(WaypointsResponseDto waypointsResponseDto) {

        Graph<GNode, GEdge> simpleGraph = new SimpleDirectedGraph<>(GEdge.class);
        waypointsResponseDto.getWaypoints().forEach(dto ->{
            simpleGraph.addVertex(new GNode(dto.getX(), dto.getZ(), dto.getY()));
        });

        waypointsResponseDto.getWaypoints().forEach(dto ->{
            Optional<GNode> v1 = findVertex(simpleGraph, dto.getX(), dto.getZ(), dto.getY());
            dto.getOut().forEach(i ->{
                WaypointDto out = waypointsResponseDto.getWaypoints().get(i-1);
                Optional<GNode> v2 = findVertex(simpleGraph, out.getX(), out.getZ(), out.getY());

                if (v1.isPresent() && v2.isPresent())
                {
                    if ( v1.get().equals(v2.get())) {
                        LOG.warn("selfloop at: {}", v1.get());
                    }
                    else {
                        simpleGraph.addEdge(v1.get(), v2.get(), new GEdge(v1.get(), v2.get()));
                    }
                }
            });
        });

//        simpleGraph.edgeSet().forEach(e ->{
//            GNode von = simpleGraph.getEdgeSource(e);
//            GNode zu = simpleGraph.getEdgeTarget(e);
//
//
//        });

        return simpleGraph;
    }

    private Optional<GNode> findVertex(Graph<GNode, GEdge> simpleGraph, double x, double y, double z) {
        return simpleGraph.vertexSet().stream().filter(v -> v.getX()==x && v.getY() == y && v.getZ() == z).findFirst();
    }



}
