package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehiclecommunication.upload.filter.ConditionType;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.config.Licence;

import java.util.List;
import java.util.stream.Collectors;

public class UploadFilterSettingsModel extends Admin {
    public final String sequenceId;
    public final List<UploadFilter> filters;
    public final ConditionType[] conditionTypes;
    public String filterEditMemo;
    public String filtersUseMemo;
    public String patternExamples;

    public UploadFilterSettingsModel(UploadFilterSequence filterSequence, Licence licence ) {
        super(licence);
        this.conditionTypes = new ConditionType[]{ConditionType.VEHICLE_NAME, ConditionType.GROUP_NAME, ConditionType.FILE_NAME};
        this.filters = filterSequence.filters.stream().map(UploadFilter::new).collect(Collectors.toList());;
        this.sequenceId = filterSequence.id.toString();
    }

    public static class UploadFilter {
        public String directory;
        public String description;
        public String delete;
        public String deleteDays;
        public List<UploadFilterCondition> conditions;

        public UploadFilter(fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter filter) {
            directory = filter.dir;
            description = filter.description;
            delete = filter.delete ? "Enabled" : "Disabled";
            deleteDays = String.valueOf(filter.deleteDays);
            conditions = filter.conditions.stream().map(UploadFilterCondition::new).collect(Collectors.toList());
        }
    }

    public static class UploadFilterCondition {
        public ConditionType type;
        public String matchString;

        public UploadFilterCondition(fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterCondition condition) {
            type = condition.type;
            matchString = condition.matchString;
        }
    }

}
