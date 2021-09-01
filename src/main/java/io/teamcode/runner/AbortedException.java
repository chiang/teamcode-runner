package io.teamcode.runner;

/**
 * CI Server 측에서 해당 Job 을 취소할 때 발생하는 Exception. 이 Exception 을 받는 쪽에서는 해당 Job 을 취소해야 합니다.
 *
 * Created by chiang on 2017. 7. 30..
 */
public class AbortedException extends Exception {
}
