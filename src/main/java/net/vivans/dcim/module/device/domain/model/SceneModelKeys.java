package net.vivans.dcim.module.device.domain.model;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 3D 도면 모델 키 규칙.
 * PRIMITIVES는 화면이 GLB 파일 없이 직접 그리는 기본 도형(벽·문·바닥 등)으로, 3D 엔진에 속한 값이라 코드에 둔다.
 * 그 밖의 모델(기본 제공 SR_* 및 업로드 USR_*)은 scene_model 카탈로그에 있어야 하며, 존재 여부는 서비스 계층이 검사한다.
 */
public final class SceneModelKeys {

    public static final Set<String> PRIMITIVES = Set.of(
            "RACK", "PDU", "CRAC", "UPS", "SENSOR", "GENERIC", "CHILLER", "SWITCH", "SERVER",
            "MACHINE", "CONVEYOR", "TANK", "TABLE", "WALL", "WALL_CONCRETE", "WALL_GLASS", "WALL_GLASS_CLEAR", "WALL_PANEL",
            "DOOR", "DOOR_DOUBLE", "DOOR_SLIDING", "WINDOW", "WINDOW_DOUBLE", "CEILING", "CEILING_CLEAR", "FLOOR", "LIGHT", "PIPE", "STAIR");

    private static final Pattern CATALOG_KEY = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");

    private SceneModelKeys() {
    }

    public static boolean isPrimitive(String modelKey) {
        return modelKey != null && PRIMITIVES.contains(modelKey);
    }

    /** 카탈로그(scene_model) 모델 키로 쓸 수 있는 형식인지. 실제 존재 여부는 별도로 확인한다. */
    public static boolean isCatalogKeyFormat(String modelKey) {
        return modelKey != null && !isPrimitive(modelKey) && CATALOG_KEY.matcher(modelKey).matches();
    }
}
