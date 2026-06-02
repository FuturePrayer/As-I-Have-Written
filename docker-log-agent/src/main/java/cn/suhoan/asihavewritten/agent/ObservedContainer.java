package cn.suhoan.asihavewritten.agent;

import java.util.Map;

public record ObservedContainer(
        String id,
        String name,
        String image,
        Map<String, String> labels) {
}
