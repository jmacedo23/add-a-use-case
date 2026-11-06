package team.projectpulse.student;

import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers the questions a route asks about one named student: is that student the caller herself
 * ({@code isStudentSelf}), and is the caller an instructor of that student's course section
 * ({@code isCurrentUserInstructorOfStudentSection}).
 *
 * <p>Every check fails closed: an id that names no row, or a link not yet established, is denied rather than
 * throwing. These methods run inside an {@code AuthorizationManager}, upstream of the {@code DispatcherServlet},
 * so an exception thrown here escapes the filter chain as a 500 that also tells the caller which ids exist.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * A course admin may manage every section in her course; an instructor may manage only students in sections to
     * which she is assigned. Missing students and incomplete associations are denied without revealing their state.
     */
    public boolean canDeactivateStudent(Integer studentId) {
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null || student.getSection() == null || student.getSection().getCourse() == null) {
            return false;
        }

        Integer actorId = this.userUtils.getUserId();
        if (student.getSection().getCourse().getCourseAdmin() != null
                && actorId.equals(student.getSection().getCourse().getCourseAdmin().getId())) {
            return true;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> actorId.equals(instructor.getId()));
    }

}
