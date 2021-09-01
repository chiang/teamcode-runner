package io.teamcode.runner.executor.docker;

import java.util.Collections;
import java.util.List;

/**
 * Created by chiang on 2017. 4. 27..
 */
public class DockerMachine {

    public List<String> list() {

        return Collections.emptyList();
    }

    public boolean canConnect() {

        return true;

        /*
        // Execute docker-machine config which actively ask the machine if it is up and online
	cmd := exec.Command("docker-machine", "config", name)
	cmd.Env = os.Environ()
	err := cmd.Run()
	if err == nil {
		return true
	}
	return false
         */
    }

    //void list();

    //List() (machines []string, err error)
}
