package io.teamcode.runner.common;

/**
 * Created by chiang on 2017. 4. 27..
 */
public enum BuildStage {

    PREPARE("prepare_script"),
    GET_SOURCES("get_sources"),
    RESTORE_CACHE("restore_cache"),
    //Download 라고 하지만 실제로는 Docker Volume 에 연결된 Artifact 를 가져오는 것입니다.
    //TODO 나중에는 다운로드 방식도 있겠지요.
    DOWNLOAD_ARTIFACTS("download_artifacts"),
    USER_SCRIPT("build_script"),
    AFTER_SCRIPT("after_script"),
    ARCHIVE_CACHE("archive_cache"),
    UPLOAD_ARTIFACTS("upload_artifacts");

    private String label;

    BuildStage(final String label) {
        this.label = label;
    }

}
