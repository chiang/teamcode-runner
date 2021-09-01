
## Startup

아래와 같이 스크립트를 만들어 실행합니다.

gradle -x test build

```
java -jar runner-1.0-SNAPSHOT.jar &
```

## Architecture

### 빌드 방식

#### Docker Container 구성

빌드는 Docker Container 내에서 처리됩니다. 동일한 Stage 내에서 여러 Job 은 동시에 처리될 수 있으며 따라서 Container 가 여러 개 생성이 
될 수 있습니다.

Docker Container 는 아래와 같은 이름으로 생성이 됩니다.

```
runner-{runner-short-token}-project-{project-id}-concurrent-{project-runner-id}
```

Runner Short Token 은 Runner 에 발급된 Token 의 Short Version 입니다. 이 Short Token 은 중복될 가능성이 거의 없기 때문에 
이렇게 사용합니다.

Project ID 는 빌드 대상 Job 의 프로젝트 ID 입니다. Project Runner ID 는 프로젝트 별로 Job 을 처리할 때 마다 순차적으로 증가하는 ID 입니다. 이 조합을 
살펴보면, 하나의 Runner 에 프로젝트 별로 여러 Build 를 실행할 수 있는 Container 를 구성하는 구조입니다.

##### Predefined Docker Image

Docker 빌드 시 Subversion Client 가 필요합니다 (소스 체크아웃 등). 이와 같이 TeamCode Runner 의 기본 기능 실행을 위한 
환경을 Docker 내부에 구성할 필요가 있는데, 이를 위해서 필요한 것이 Predefined Docker Image 입니다 Predefined Image 에서는 
아래와 같은 기능을 구성합니다

1. Subversion Client 1.9.x
2. Python (for Python Fabric?, Deploy)



#### Build Directory Structure````

빌드할 때는 매번 소스 코드를 체크아웃합니다. 이 소스 코드는 Job 마다 저장이 되며 경로는 아래와 같습니다. 

```
$TEAMCODE_HOME/data/cache/runner-[runner-id]-project-[project-id]-concurrent-[job-id]
```


## Build

Cache 디렉터리에 Checkout 한 파일을 저장합니다. 동시에 여러 Job 이 Checkout 을 할 수 있으므로 이 점을 고려해서 설계해야 합니다. 
Job ID 는 전체 시스템 상에서 Unique 하므로 아래와 같이 Cache 디렉터리를 만들면 절대로 중복될 일이 없습니다.

```
$TEAMCODE_HOME/data/cache/runner-[runner-id]-project-[project-id]-concurrent-[job-id]
```

## Releases

### 1.0

### 1.1

Docker Client 를 8.8.3 으로 업그레이드 (8.7.3 에서)