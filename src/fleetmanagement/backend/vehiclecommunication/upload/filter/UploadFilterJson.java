package fleetmanagement.backend.vehiclecommunication.upload.filter;

import gsp.util.DoNotObfuscate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@DoNotObfuscate
public class UploadFilterJson {
    public String description;
    public List<UploadFilterConditionJson> conditions = new ArrayList<>();
    public String dir;
    public String name;
    public String delete;
    public String deleteDays;

    public UploadFilter toFilter() {
        return new UploadFilter(name, dir, description, delete, deleteDays,
                conditions.stream().map(UploadFilterConditionJson::toCondition).collect(Collectors.toList()));
    }

    @DoNotObfuscate
    public static class UploadFilterConditionJson {
        public ConditionType type;
        public String matchString;

        public UploadFilterCondition toCondition() {
            return new UploadFilterCondition(type, matchString);
        }
    }
}
