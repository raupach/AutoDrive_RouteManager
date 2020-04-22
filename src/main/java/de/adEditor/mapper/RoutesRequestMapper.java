package de.adEditor.mapper;

import de.adEditor.dto.Group;
import de.adEditor.dto.Marker;
import de.adEditor.dto.RouteExport;
import de.adEditor.dto.Waypoints;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.GroupDto;
import de.autoDrive.NetworkServer.rest.dto_v1.MarkerDto;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesRequestDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoutesRequestMapper {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(RoutesRestPath.DATE_FORMAT);

    public RoutesRequestDto toDto(RouteExport routeExport, String name, String map, Integer revision, Date date, String username, String description) {
        RoutesRequestDto dto = new RoutesRequestDto();
        dto.setDate(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(formatter));
        dto.setName(name);
        dto.setMap(map);
        dto.setRevision(revision);
        dto.setUsername(username);
        dto.setDescription(StringUtils.substring(description, 0, 2999));
        dto.setGroups(routeExport.getGroups().stream().map(group -> toGroupDto(group)).collect(Collectors.toList()));
        dto.setMarkers(routeExport.getMarkers().stream().map(m -> toMarkerDto(m)).collect(Collectors.toList()));
        dto.setWaypoints(toWaypointDtos(routeExport.getWaypoints()));
        return dto;
    }

    private MarkerDto toMarkerDto(Marker marker) {
        var dto = new MarkerDto();
        dto.setGroup(marker.getGroup());
        dto.setName(marker.getName());
        dto.setWaypointIndex(marker.getWaypointIndex());
        return dto;
    }

    private GroupDto toGroupDto(Group group) {
        var dto = new GroupDto();
        dto.setName(group.getName());
        return dto;
    }

    private List<WaypointDto> toWaypointDtos(Waypoints waypoints) {

        // TODO: remove stream
        List<WaypointDto> dtos = Arrays.stream(waypoints.getX().split(";")).map(x -> {
            WaypointDto wp = new WaypointDto();
            wp.setX(Double.valueOf(x));
            return wp;
        }).collect(Collectors.toList());

        String[] y = waypoints.getY().split(";");
        String[] z = waypoints.getZ().split(";");
        String[] out = waypoints.getOut().split(";");

        for (int i = 0; i < dtos.size(); i++) {
            WaypointDto dto = dtos.get(i);
            dto.setY(Double.valueOf(y[i]));
            dto.setZ(Double.valueOf(z[i]));
            String[] oValue = out[i].split(",");
            for (String s : oValue) {
                if (StringUtils.isNotBlank(s)) {
                    dto.getOut().add(Integer.valueOf(s));
                }
            }
        }

        return dtos;
    }
}
