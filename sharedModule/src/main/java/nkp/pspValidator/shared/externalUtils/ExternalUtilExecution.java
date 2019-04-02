package nkp.pspValidator.shared.externalUtils;

import java.util.Objects;

public class ExternalUtilExecution {
    private final String name;
    private final ExternalUtil util;

    public ExternalUtilExecution(String name, ExternalUtil util) {
        this.name = name;
        this.util = util;
    }

    public String getName() {
        return name;
    }

    public ExternalUtil getUtil() {
        return util;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalUtilExecution that = (ExternalUtilExecution) o;
        return Objects.equals(name, that.name) &&
                util == that.util;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, util);
    }

    @Override
    public String toString() {
        return "ExternalUtilExecution{" +
                "name='" + name + '\'' +
                ", util=" + util +
                '}';
    }
}
