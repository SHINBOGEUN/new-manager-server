package net.vivans.dcim.module.device.domain.model;

/**
 * 3D 도면에서 모델이 맡는 역할. 모델 키(SR_* 등)의 이름 규칙이 아니라 scene_model 카탈로그의 값으로 판단한다.
 * <ul>
 *   <li>RACK  - 랙처럼 동작: 위치(LOCATION_TYPE=RACK) 연결, 랙 색상 지정, 랙 내부 장비 표시</li>
 *   <li>PANEL - 분전반: 분전반 구성품 배치, 분전반 사진·배치 저장</li>
 *   <li>FRAME - 설비 프레임: 높이·깊이를 폭과 별개로 키울 수 있는 구조물</li>
 * </ul>
 */
public enum SceneModelRole {
    RACK,
    PANEL,
    FRAME
}
