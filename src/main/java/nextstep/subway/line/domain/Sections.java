package nextstep.subway.line.domain;

import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Embeddable
public class Sections {

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<Section> sections = new ArrayList<>();

    public Sections() {

    }

    public void add(Section section) {
        if (sections.isEmpty()) {
            sections.add(section);
            return;
        }

        validateAddSection(section);

        if (addSectionInTheMiddle(section)) {
            return;
        }

        sections.add(section);
    }

    private void validateAddSection(Section section) {
        validateToAddSectionStationsAlreadyAdded(section);
        validateToAddSectionStationNone(section);
    }

    public List<Section> getAll() {
        return sections;
    }

    public List<Station> getStations() {
        if (getAll().isEmpty()) {
            return Arrays.asList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation();
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = getAll().stream()
                    .filter(it -> it.getUpStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    private Station findUpStation() {
        Station downStation = getAll().get(0).getUpStation();
        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = getAll().stream()
                    .filter(it -> it.getDownStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }


    private boolean addSectionInTheMiddle(Section section) {
        if (addSectionInTheMiddleToUpStation(section)) {
            return true;
        }

        return addSectionInTheMiddleToDownStation(section);
    }

    private boolean addSectionInTheMiddleToUpStation(Section section) {
        Section upStationMatchedSection = sections.stream()
                .filter(it -> it.getUpStation() == section.getUpStation())
                .findFirst()
                .orElse(null);

        if (upStationMatchedSection != null) {
            validateToAddSectionDistance(upStationMatchedSection, section);

            Section afterSection = new Section(
                    section.getLine(),
                    section.getDownStation(),
                    upStationMatchedSection.getDownStation(),
                    upStationMatchedSection.getDistance() - section.getDistance()
            );

            sections.remove(upStationMatchedSection);
            sections.add(section);
            sections.add(afterSection);

            return true;
        }

        return false;
    }

    private boolean addSectionInTheMiddleToDownStation(Section section) {
        Section downStationMatchedSection = sections.stream()
                .filter(it -> it.getDownStation() == section.getDownStation())
                .findFirst()
                .orElse(null);

        if (downStationMatchedSection != null) {
            validateToAddSectionDistance(downStationMatchedSection, section);

            Section beforeSection = new Section(
                    section.getLine(),
                    downStationMatchedSection.getUpStation(),
                    section.getUpStation(),
                    downStationMatchedSection.getDistance() - section.getDistance()
            );

            sections.remove(downStationMatchedSection);
            sections.add(section);
            sections.add(beforeSection);

            return true;
        }

        return false;
    }

    private void validateToAddSectionDistance(Section section, Section toAddSection) {
        if (toAddSection.getDistance() >= section.getDistance()) {
            throw new RuntimeException();
        }
    }

    private void validateToAddSectionStationsAlreadyAdded(Section toAddSection) {
        if (getStations().containsAll(toAddSection.getStations())) {
            throw new RuntimeException();
        }
    }

    private void validateToAddSectionStationNone(Section toAddSection) {
        if (!getStations().contains(toAddSection.getUpStation()) &&
                !getStations().contains(toAddSection.getDownStation())) {

            throw new RuntimeException();
        }
    }

    public void remove(Station station) {
        validateToRemove(station);
        removeStation(station);
    }

    private void validateToRemove(Station station) {
        if (sections.size() <= 1) {
            throw new RuntimeException();
        }

        getStations().stream()
                .filter(it -> it == station)
                .findFirst()
                .orElseThrow(() -> new RuntimeException());
    }

    private void removeStation(Station station) {
        Section upStationMatchedSection = sections.stream()
                .filter(it -> it.getUpStation() == station)
                .findFirst()
                .orElse(null);

        Section downStationMatchedSection = sections.stream()
                .filter(it -> it.getDownStation() == station)
                .findFirst()
                .orElse(null);

        if (upStationMatchedSection != null && downStationMatchedSection == null) {
            sections.remove(upStationMatchedSection);
            return;
        }

        if (upStationMatchedSection == null && downStationMatchedSection != null) {
            sections.remove(downStationMatchedSection);
            return;
        }

        Section mergedSection = new Section(
                upStationMatchedSection.getLine(),
                downStationMatchedSection.getUpStation(),
                upStationMatchedSection.getDownStation(),
                upStationMatchedSection.getDistance() + downStationMatchedSection.getDistance()
        );

        sections.remove(upStationMatchedSection);
        sections.remove(downStationMatchedSection);
        sections.add(mergedSection);
    }
}
