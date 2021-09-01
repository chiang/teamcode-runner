package io.teamcode.runner.network;

/**
 * Created by chiang on 2017. 4. 29..
 */
public class TeamCodeClient {

    /**
     * TeamCode CI 측에 요청한 후 받은 응답 Header 의 값. 이 값으로 마지막 요청한 시각을 추적합니다.
     *
     * Header 는 X-TeamCode-Last-Update
     *
     * //X-GitLab-Last-Update
     *
     */
    private String lastUpdate;

    public String getLastUpdate() {

        return this.lastUpdate;
    }
}
