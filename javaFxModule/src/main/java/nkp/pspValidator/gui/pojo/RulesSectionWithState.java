package nkp.pspValidator.gui.pojo;

import nkp.pspValidator.shared.engine.RulesSection;

import java.util.Random;

/**
 * Created by martin on 16.12.16.
 */
public class RulesSectionWithState {

    private final RulesSection section;
    private ProcessingState state = randomState();//TMP

    private ProcessingState randomState() {
        Random random = new Random(System.currentTimeMillis());
        return ProcessingState.values()[random.nextInt(3)];
    }

    private Integer errors = randomNum();
    private Integer warnings = randomNum();
    private Integer infos = randomNum();

    private Integer randomNum() {
        Random random = new Random();
        switch (random.nextInt(4)) {
            case 0:
                return 0;
            case 1:
                return random.nextInt(10);
            case 2:
                return 10 + random.nextInt(90);
            default:
                return 100 + random.nextInt(900);
        }
    }


    public RulesSectionWithState(RulesSection section) {
        this.section = section;
    }

    public RulesSection getSection() {
        return section;
    }

    public ProcessingState getState() {
        return state;
    }

    public void setState(ProcessingState state) {
        this.state = state;
    }

    public Integer getErrors() {
        return errors;
    }

    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public void setWarnings(Integer warnings) {
        this.warnings = warnings;
    }

    public Integer getInfos() {
        return infos;
    }

    public void setInfos(Integer infos) {
        this.infos = infos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RulesSectionWithState that = (RulesSectionWithState) o;

        return section.equals(that.section);

    }

    @Override
    public int hashCode() {
        return section.hashCode();
    }
}
