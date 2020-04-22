package de.adEditor.mapper;

import de.adEditor.dto.Group;
import de.adEditor.dto.Marker;
import de.adEditor.dto.RouteExport;
import de.adEditor.dto.Waypoints;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RouteExportMapper {

    public RouteExport toRouteExport(WaypointsResponseDto waypointsResponseDto) {
        int waypointCount = waypointsResponseDto.getWaypoints().size();
        RouteExport routeExport = new RouteExport();
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<Double> z = new ArrayList<>();
        List<String> out = new ArrayList<>();
        List<WaypointDto> waypointDtos = waypointsResponseDto.getWaypoints();
        Map<Integer, List<Integer>> inMap = new HashMap<>();

        // Fill in-Map with default-value -1. In case of that we have no referencing out-node.
        for (int i = 1; i <= waypointCount; i++) {
            List<Integer> initList = new ArrayList<>();
            initList.add(-1);
            inMap.put(i, initList);
        }

        waypointDtos.forEach(waypointDto -> {

            x.add(waypointDto.getX());
            y.add(waypointDto.getY());
            z.add(waypointDto.getZ());

            out.add(waypointDto.getOut().isEmpty() ? "-1" : StringUtils.join(waypointDto.getOut(), ","));

            waypointDto.getOut().forEach(i -> {
                List<Integer> in = inMap.get(i);

                // remove the default value if its alone.
                if (in.size() == 1 && in.get(0).equals(-1)) {
                    in.clear();
                }
                in.add((waypointDtos.indexOf(waypointDto)) + 1);
            });
        });

        Waypoints waypoints = new Waypoints();
        waypoints.setC(waypointCount);
        waypoints.setX(StringUtils.join(x, ";"));
        waypoints.setY(StringUtils.join(y, ";"));
        waypoints.setZ(StringUtils.join(z, ";"));
        waypoints.setOut(StringUtils.join(out, ";"));
        waypoints.setIn(StringUtils.join(inMap.values().stream().map(inIndex -> StringUtils.join(inIndex, ",")).collect(Collectors.toList()), ";"));

        routeExport.setWaypoints(waypoints);
        routeExport.setGroups(waypointsResponseDto.getGroups().stream().map(g -> new Group(g.getName())).collect(Collectors.toList()));
        routeExport.setMarkers(waypointsResponseDto.getMarkers().stream().map(m -> new Marker(m.getName(), m.getGroup(), m.getWaypointIndex() + 1)).collect(Collectors.toList()));
        return routeExport;
    }
}
