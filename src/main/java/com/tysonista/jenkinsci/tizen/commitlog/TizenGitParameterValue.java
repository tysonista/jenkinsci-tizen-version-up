package com.tysonista.jenkinsci.tizen.commitlog;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.StringParameterValue;

public class TizenGitParameterValue extends StringParameterValue {

    /**
     * 
     */
    private static final long serialVersionUID = -41966854759221124L;
    private String url;
    private String port;
    private String path;
    private String branch;

    protected TizenGitParameterValue(String name, String value, String url, String port, String path, String branch) {
        super(name, value);
        this.url = url;
        this.port = port;
        this.path = path;
        this.branch = branch;
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        env.put("Git_Url", url);
        env.put("Git_Port", port);
        env.put("Git_Path", path);
        env.put("Git_Branch", branch);
        super.buildEnvironment(build, env);
    }
}
