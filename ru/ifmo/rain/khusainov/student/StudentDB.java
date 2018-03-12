package ru.ifmo.rain.khusainov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private static final Comparator<Student> BY_NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Comparator.comparing(Student::getFirstName))
            .thenComparing(Comparator.comparingInt(Student::getId));

    private List<String> getStudentsInfo(List<Student> students, Function<Student, String> mapper) {
        return students.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsInfo(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsInfo(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getStudentsInfo(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsInfo(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    private Stream<Student> sortByComparator(Stream<Student> students, Comparator<Student> comparator) {
        return students
                .sorted(comparator);
    }

    private List<Student> sortByComparator(Collection<Student> students, Comparator<Student> comparator) {
        return sortByComparator(students.stream(), comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortByComparator(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortByComparator(students, BY_NAME_COMPARATOR);
    }

    private Stream<Student> findStudentsByPredicate(Stream<Student> students, Predicate<Student> predicate) {
        return students
                .filter(predicate);
    }

    private List<Student> findStudentsByPredicate(Collection<Student> students, Predicate<Student> predicate) {
        return findStudentsByPredicate(students.stream(), predicate)
                .collect(Collectors.toList());
    }

    private Predicate<Student> getPredicateByFunction(Function<Student, String> function, String parameter) {
        return student -> function.apply(student).equals(parameter);
    }

    private List<Student> findStudentsAndSort(Collection<Student> students, Function<Student, String> f, String value) {
        return sortByComparator(
                findStudentsByPredicate(students.stream(),
                        getPredicateByFunction(f, value)),
                BY_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsAndSort(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsAndSort(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsAndSort(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByPredicate(students.stream(), getPredicateByFunction(Student::getGroup, group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupStream(Collection<Student> students, Supplier<Map<String, List<Student>>> mapType) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, mapType, Collectors.toList()))
                .entrySet()
                .stream();
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupStream(Collection<Student> students) {
        return getGroupStream(students, HashMap::new);
    }

    private Stream<Map.Entry<String, List<Student>>> getSortedGroupStream(Collection<Student> students) {
        return getGroupStream(students, TreeMap::new);
    }

    private List<Group> getSortedListOfGroupsByInnerComparator(Collection<Student> students, Comparator<Student> comparator) {
        return getSortedGroupStream(students)
                .peek((e) -> e.getValue().sort(comparator))
                .map((e) -> new Group(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedListOfGroupsByInnerComparator(students, BY_NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedListOfGroupsByInnerComparator(students, Comparator.comparingInt(Student::getId));
    }

    private String getGroupWithMaxByComparator(Collection<Student> students, Comparator<Map.Entry<String, List<Student>>> comparator) {
        return getGroupStream(students)
                .max(comparator
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getGroupWithMaxByComparator(students, Comparator.comparingInt(e -> e.getValue().size()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getGroupWithMaxByComparator(
                students,
                Comparator.comparingLong(
                        e -> e.getValue().stream()
                                .map(Student::getFirstName)
                                .distinct()
                                .count()
                )
        );
    }
}