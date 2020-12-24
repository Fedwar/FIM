package fleetmanagement.backend.repositories.exception;

public class GroupDuplicationException extends RuntimeException {

    private static final long serialVersionUID = 2908843985601940360L;

    public GroupDuplicationException(String groupName) {
        super(String.format("The group with name %s already exists.", groupName));
    }
}
