package nkp.pspValidator.shared.imageUtils.validation;

import nkp.pspValidator.shared.engine.exceptions.ImageUtilOutputParsingException;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

/**
 * Created by Martin Řehánek on 18.11.16.
 */
public class J2kTextProfile extends J2kProfile {

    public J2kTextProfile(ImageUtilManager imageUtilManager, ImageUtil imageUtil) {
        super(imageUtilManager, imageUtil);
    }

    @Override
    Object processImageUtilOutput(String toolRawOutput, ImageUtil util) throws ImageUtilOutputParsingException {
        return toolRawOutput;
    }
}
