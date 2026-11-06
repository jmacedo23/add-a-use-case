package team.projectpulse.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);

    /**
     * Resolves a student only when the actor owns the student's course or is assigned to the student's section.
     * Keeping this lookup scoped makes the service-layer authorization independent of the HTTP route guard.
     */
    @Query("""
            select distinct student from Student student
            join student.section section
            join section.course course
            left join section.instructors instructor
            where student.id = :studentId
              and (course.courseAdmin.id = :actorId or instructor.id = :actorId)
            """)
    Optional<Student> findDeactivatableById(@Param("studentId") Integer studentId, @Param("actorId") Integer actorId);
}
