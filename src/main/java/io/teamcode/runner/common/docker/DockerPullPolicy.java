package io.teamcode.runner.common.docker;

/**
 * https://docs.gitlab.com/runner/executors/docker.html#how-pull-policies-work
 */
public enum DockerPullPolicy {

    /**
     * The never pull policy disables images pulling completely.
     * If you set the pull_policy parameter of a Runner to never,
     * then users will be able to use only the images that have been manually pulled on the docker host the Runner runs on.
     *
     * If an image cannot be found locally, then the Runner will fail the build with an error similar to:
     * <code>
     *     Pulling docker image local_image:latest ...
     *     ERROR: Build failed: Error: image local_image:latest not found
     * </code>
     *
     * <h2>When to use this pull policy?</h2>
     * <p>
     *     This pull policy should be used if you want or need to have a full control on
     *     which images are used by the Runner's users. It is a good choice for private Runners
     *     that are dedicated to a project where only specific images can be used (not publicly available on any registries).
     * </p>
     */
    NEVER,

    /**
     * When the if-not-present pull policy is used, the Runner will first check if the image is present locally.
     * If it is, then the local version of image will be used. Otherwise, the Runner will try to pull the image.
     */
    IF_NOT_PRESENT,

    /**
     * The always pull policy will ensure that the image is always pulled.
     * When always is used, the Runner will try to pull the image even if a local copy is available.
     * If the image is not found, then the build will fail with an error similar to:
     */
    ALWAYS

}
