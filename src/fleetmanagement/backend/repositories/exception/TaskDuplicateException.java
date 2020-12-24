package fleetmanagement.backend.repositories.exception;

import java.util.UUID;

public class TaskDuplicateException extends RuntimeException {

	private static final long serialVersionUID = 5291344378245558870L;

	public TaskDuplicateException(UUID taskId) {
		super(String.format("Task with ID %s already exists.", taskId));
	}
}
