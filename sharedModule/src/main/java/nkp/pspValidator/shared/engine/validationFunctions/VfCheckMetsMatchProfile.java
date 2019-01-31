package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckMetsMatchProfile extends ValidationFunction {

    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_PROFILE = "profile";


    public VfCheckMetsMatchProfile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
                .withValueParam(PARAM_PROFILE, ValueType.STRING, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            //mets files
            //TODO: this construct to merge files from multiple parameters is used multiple times - DRY
            Set<File> metsFiles = new HashSet<>();

            List<ValueParam> fileParams = valueParams.getParams(PARAM_METS_FILE);
            for (ValueParam fileParam : fileParams) {
                ValueEvaluation fileEvaluation = fileParam.getEvaluation();
                File fileFromFileParam = (File) fileEvaluation.getData();
                if (fileFromFileParam == null) {
                    return invalidValueParamNull(PARAM_METS_FILE, fileEvaluation);
                } else {
                    metsFiles.add(fileFromFileParam.getAbsoluteFile());
                }
            }

            List<ValueParam> filesParams = valueParams.getParams(PARAM_METS_FILES);
            for (ValueParam fileParam : filesParams) {
                ValueEvaluation filesEvaluation = fileParam.getEvaluation();
                List<File> filesFromFilesParam = (List<File>) filesEvaluation.getData();
                if (filesFromFilesParam == null) {
                    return invalidValueParamNull(PARAM_METS_FILES, filesEvaluation);
                } else {
                    for (File file : filesFromFilesParam) {
                        metsFiles.add(file.getAbsoluteFile());
                    }
                }
            }

            //profile
            ValueEvaluation filegroupIdParam = valueParams.getParams(PARAM_PROFILE).get(0).getEvaluation();
            String profileId = (String) filegroupIdParam.getData();
            if (profileId == null) {
                return invalidValueParamNull(PARAM_PROFILE, filegroupIdParam);
            }

            return validate(metsFiles, profileId);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(Set<File> metsFiles, String profileId) {
        ValidationResult result = new ValidationResult();
        try {
            MetadataProfile profile = engine.getMetsProfilesManager().buildProfile(profileId);
            if (profile == null) {
                result.addError(invalid(Level.ERROR, "nenalezen profil '%s' pro validaci METS záznamů", profileId));
            } else {
                for (File metFile : metsFiles) {
                    validateMets(profile, metFile, result);
                }
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        }
        return result;
    }

    private void validateMets(MetadataProfile profile, File metsFile, ValidationResult result) throws XmlFileParsingException, XPathExpressionException, InvalidXPathExpressionException {
        Document metsDoc = engine.getXmlDocument(metsFile, true);
        MetadataProfileValidator.validate(profile, metsDoc, result, metsFile.getName());
    }

}
