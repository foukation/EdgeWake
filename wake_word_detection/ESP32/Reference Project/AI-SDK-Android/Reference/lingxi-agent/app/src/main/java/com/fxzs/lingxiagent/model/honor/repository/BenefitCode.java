package com.fxzs.lingxiagent.model.honor.repository;

import java.util.Objects;

public class BenefitCode {

    public static final String NOT_SUBSCRIBE = "6403";
    public static final String SERVICE_ERROR_1 = "6404";
    public static final String EXPIRED = "6405";
    public static final String NOT_EFFECTIVE = "6406";
    public static final String LIMIT_REACHED = "6407";
    public static final String SERVICE_ERROR_2 = "6500";
    public static final String SERVICE_ERROR_3 = "6408";
    public static final String SERVICE_ERROR_4 = "6409";

    public static boolean isBenefitError(String code) {
        return Objects.equals(code, NOT_SUBSCRIBE)
                || Objects.equals(code, SERVICE_ERROR_1)
                || Objects.equals(code, EXPIRED)
                || Objects.equals(code, NOT_EFFECTIVE)
                || Objects.equals(code, LIMIT_REACHED)
                || Objects.equals(code, SERVICE_ERROR_2)
                || Objects.equals(code, SERVICE_ERROR_3)
                || Objects.equals(code, SERVICE_ERROR_4);
    }
}
