package ru.ifmo.rain.khusainov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private Comparator<Student> byNameComparator = Comparator
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
        return new TreeSet<>(getFirstNames(students));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Student::compareTo)
                .get() //throws NoSuchElementException but it's ok, what it should do else?
                .getFirstName();
    }

    private List<Student> sortByComparator(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortByComparator(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortByComparator(students, byNameComparator);
    }

    private List<Student> findStudentsByPredicate(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByPredicate(sortStudentsByName(students), s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByPredicate(sortStudentsByName(students), s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsByPredicate(sortStudentsByName(students), s -> s.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName, BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private Stream<Map.Entry<String, List<Student>>> getListGroupStream(Collection<Student> students){
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet()
                .stream();
    }

    private List<Group> getGroupsByComparator(Collection<Student> students, Comparator<Student> comparator) {
        return getListGroupStream(students)
                .peek((e) -> e.getValue().sort(comparator))
                .map((e) -> new Group(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsByComparator(students, byNameComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsByComparator(students, Comparator.comparingInt(Student::getId));
    }

    private String getGroupWithMaxValue(Collection<Student> students, Comparator<Map.Entry<String, List<Student>>> value) {
        return getListGroupStream(students)
                .max(value)
                .get()
                .getKey();
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getGroupWithMaxValue(students, Comparator.comparingInt(e -> e.getValue().size()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getGroupWithMaxValue(students, Comparator.comparingLong(e -> e.getValue().stream()
                .map(Student::getFirstName)
                .distinct()
                .count()));
    }
}