# 자산 이미지·문서 파일 저장 운영 가이드

자산 이미지와 첨부 문서는 MariaDB에 바이너리를 저장하지 않는다. DB에는 파일의 원본명, MIME type, 크기, 저장 키만 보관하고 실제 파일은 서버 파일 시스템의 영속 경로에 저장한다.

## 환경 변수

운영 서버에서는 애플리케이션 작업 디렉터리가 아닌 별도 영속 볼륨을 지정한다.

```powershell
$env:ASSET_IMAGE_STORAGE_PATH = "D:\dcim-data\assets\images"
$env:ASSET_DOCUMENT_STORAGE_PATH = "D:\dcim-data\assets\documents"
```

Docker 배포에서는 위 경로를 컨테이너 외부 볼륨으로 마운트한다. 기본 개발 경로는 각각 `./uploads/device-images`, `./uploads/device-documents`다.

## 백업·복구

1. MariaDB 백업에는 `device_image`, `device_asset_document` 메타데이터가 포함되어야 한다.
2. 동일한 시점의 이미지·문서 저장 경로 전체를 함께 백업한다.
3. 복구 시 DB를 복구한 뒤 동일한 경로에 파일을 복원한다.
4. 파일만 복구하거나 DB만 복구하면 미리보기·다운로드가 실패할 수 있다.

첨부 문서는 파일당 최대 20MB다. 파일 형식은 제한하지 않으며, 업로드·삭제 이력은 자산 변경 이력에서 확인한다.
