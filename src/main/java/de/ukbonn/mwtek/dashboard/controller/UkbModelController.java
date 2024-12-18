/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboard.controller;

import static de.ukbonn.mwtek.dashboard.controller.DataRetrievalController.DAYS_DIFFERENCE;
import static de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType.CLAPP;
import static de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType.PDMS_REPORTING_DB;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.extractCoreBaseDataOfFacilityEncounters;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.BODY_WEIGHT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.CREATININE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.ENCOUNTER;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.EPISODES;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.LACTATE;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.START_REPLACEMENT;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.UREA;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters.URINE_OUTPUT;

import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.Interval;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import de.ukbonn.mwtek.utilities.generic.time.TimerTools;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

@Slf4j
public class UkbModelController {

  public static Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> generateUkbModelData(
      boolean benchMarkRun,
      AbstractDataRetrievalService dataRetrievalService,
      ProcessTimer processTimer,
      ServerTypeEnum serverTypeEnum)
      throws SearchException {
    AcuwaveSearchConfiguration acuwaveSearchConfiguration;

    // Initialize output map
    Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>>
        renalReplacementModelParameterSetMap = new HashMap<>();

    if (serverTypeEnum == ServerTypeEnum.ACUWAVE) {
      acuwaveSearchConfiguration =
          ((AcuwaveDataRetrievalService) dataRetrievalService).getAcuwaveSearchConfiguration();
    } else {
      // Not implemented yet
      throw new UnsupportedOperationException(
          "Currently, UKB prediction model data generation is not supported on a non-Acuwave "
              + "workflow.");
    }

    // Fetching data
    processTimer.startLoggingTime(
        ResourceType.Encounter, "fetching icu encounters and reducing it to case id.");
    // Retrieve the local case ids of the encounter for further queries.
    renalReplacementModelParameterSetMap.put(
        ENCOUNTER, extractCoreBaseDataOfFacilityEncounters(dataRetrievalService));
    // The case ids from the previous data retrieval step are the main input parameters for the
    // following queries.
    Set<String> icuLocalCaseIds =
        renalReplacementModelParameterSetMap.get(ENCOUNTER).parallelStream()
            .map(CoreBaseDataItem::hisCaseId)
            .collect(Collectors.toSet());
    processTimer.stopLoggingTime(icuLocalCaseIds);

    processTimer.startLoggingTime(ResourceType.EpisodeOfCare, "icu episodes");
    renalReplacementModelParameterSetMap.put(
        EPISODES, dataRetrievalService.getIcuEpisodes(icuLocalCaseIds));
    processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(EPISODES));

    // Cases without episodes are not usable at all (since its impossible to calculate 24h
    // intervals), so we can filter them directly
    log.debug("Number of icu case ids before episode filtering: {}", icuLocalCaseIds.size());
    Set<String> casesWithEpisodes =
        renalReplacementModelParameterSetMap.get(EPISODES).stream()
            .map(CoreBaseDataItem::hisCaseId)
            .collect(Collectors.toSet());
    if (casesWithEpisodes.isEmpty()) {
      log.error("Unable to find any episode data. Skipping the generation of ukb model data.");
      return null;
    }
    icuLocalCaseIds.retainAll(casesWithEpisodes);
    log.debug("Number of icu case ids after episode filtering: " + icuLocalCaseIds.size());

    // Remove the encounter that got no icu episodes from the global list.
    List<CoreBaseDataItem> encounterWithIcuEpisodes =
        renalReplacementModelParameterSetMap.get(ENCOUNTER).parallelStream()
            .filter(x -> icuLocalCaseIds.contains(x.hisCaseId()))
            .toList();
    renalReplacementModelParameterSetMap.put(ENCOUNTER, encounterWithIcuEpisodes);

    // For current data we need to retrieve the data via clapp.
    // Cases without discharge date should be active and for this filtering on cases with a
    // discharge date older than 1 calendar day.
    List<String> recentCaseIds =
        renalReplacementModelParameterSetMap.get(ENCOUNTER).parallelStream()
            .filter(
                x ->
                    x.dateTo() == null
                        || !(DateTools.isMoreThanXDaysOlder(
                            DateTools.getCurrentDateTime(), x.dateTo(), DAYS_DIFFERENCE)))
            .map(CoreBaseDataItem::hisCaseId)
            .toList();

    List<String> icuLocalCaseIdsWithoutRecents =
        icuLocalCaseIds.stream().filter(x -> !recentCaseIds.contains(x)).toList();
    processTimer.stopLoggingTime(icuLocalCaseIds);

    log.debug("The amount of ALL cases is: " + icuLocalCaseIds.size());
    if (!recentCaseIds.isEmpty()) {
      log.debug(
          "The amount of current cases is: "
              + recentCaseIds.size()
              + " [example case: "
              + recentCaseIds.get(0)
              + " ]");
    }
    if (!icuLocalCaseIdsWithoutRecents.isEmpty()) {
      log.debug(
          "The amount of ALL cases without current cases is: "
              + icuLocalCaseIdsWithoutRecents.size()
              + " [example case: "
              + icuLocalCaseIdsWithoutRecents.get(0)
              + " ]");
    }

    // Retrieval of the ICU EPISODE information (including internal case ids) which is needed for
    // the detection of deltas in periods of vital signs
    // Retrieval of the observations codes via orbis internal code and filtering on the forced
    // loinc code right after, since there could be ones with another unit.
    processTimer.startLoggingTime(ResourceType.Observation, "lab value: lactate");
    renalReplacementModelParameterSetMap.put(
        LACTATE,
        dataRetrievalService.getUkbRenalReplacementObservations(
            icuLocalCaseIds,
            acuwaveSearchConfiguration.getPredictionModelUkbObservationOrbisLactateCodes()));
    processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(LACTATE));

    processTimer.startLoggingTime(ResourceType.Observation, "lab value: creatinine");
    renalReplacementModelParameterSetMap.put(
        CREATININE,
        dataRetrievalService.getUkbRenalReplacementObservations(
            icuLocalCaseIds,
            acuwaveSearchConfiguration.getPredictionModelUkbObservationOrbisCreatinineCodes()));
    processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(CREATININE));

    processTimer.startLoggingTime(ResourceType.Observation, "lab value: urea");
    renalReplacementModelParameterSetMap.put(
        UREA,
        dataRetrievalService.getUkbRenalReplacementObservations(
            icuLocalCaseIds,
            acuwaveSearchConfiguration.getPredictionModelUkbObservationOrbisUreaCodes()));
    processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(UREA));
    // Removal of all values that have no intersection with an icu episode of care of the
    // given case
    renalReplacementModelParameterSetMap =
        removeNonIcuLabValues(renalReplacementModelParameterSetMap);

    Set<String> caseIdsWithAllLabValues =
        getCaseIdsWithAllLabValues(renalReplacementModelParameterSetMap);

    // processTimer.stopLoggingTime(listUkbObservations);
    Set<String> filteredCaseIds =
        icuLocalCaseIds.stream()
            .filter(caseIdsWithAllLabValues::contains)
            .collect(Collectors.toSet());

    Set<String> filteredRecentCaseIds =
        recentCaseIds.stream()
            .filter(caseIdsWithAllLabValues::contains)
            .collect(Collectors.toSet());

    Set<String> filteredCaseIdsWithoutRecents =
        icuLocalCaseIdsWithoutRecents.stream()
            .filter(caseIdsWithAllLabValues::contains)
            .collect(Collectors.toSet());

    for (String caseId : icuLocalCaseIds) {
      if (!filteredCaseIds.contains(caseId)) {
        log.trace("Couldn't find an observation with lab value information for case: " + caseId);
      }
    }

    if (benchMarkRun) {
      processTimer.startLoggingTime(ResourceType.Observation, "bodyWeight clapp ALL");
      // Retrieval of the body weight
      //        List<UkbObservation> bodyWeightResources =
      //            (List<UkbObservation>) Converter.convert(
      //                dataRetrievalService.getUkbRenalReplacementBodyWeight(icuLocalCaseIds));
      renalReplacementModelParameterSetMap.put(
          BODY_WEIGHT,
          dataRetrievalService.getUkbRenalReplacementBodyWeight(filteredCaseIds, CLAPP));
      //        renalReplacementModelParameterSetMap.put(BODY_WEIGHT,
      //            dataRetrievalService.getUkbRenalReplacementBodyWeight(filteredCaseIds,
      //                PDMS_REPORTING_DB));
      processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(BODY_WEIGHT));
    }

    log.debug("Input CLAPP: {}", filteredCaseIds.size());
    log.debug("Input PDMS: {}", filteredCaseIdsWithoutRecents.size());

    processTimer.startLoggingTime(ResourceType.Observation, "bodyWeightPdms");
    List<CoreBaseDataItem> bodyWeightPdms =
        dataRetrievalService.getUkbRenalReplacementBodyWeight(
            filteredCaseIdsWithoutRecents, PDMS_REPORTING_DB);
    if (!renalReplacementModelParameterSetMap.containsKey(BODY_WEIGHT)) {
      renalReplacementModelParameterSetMap.put(BODY_WEIGHT, bodyWeightPdms);
    } else {
      renalReplacementModelParameterSetMap
          .get(BODY_WEIGHT)
          .addAll(
              getUniqueItemsByHisCaseId(
                  renalReplacementModelParameterSetMap.get(BODY_WEIGHT), bodyWeightPdms));
    }
    processTimer.stopLoggingTime(bodyWeightPdms);

    processTimer.startLoggingTime(ResourceType.Observation, "bodyWeight clapp RECENT");
    // Retrieval of the body weight
    //        List<UkbObservation> bodyWeightResources =
    //            (List<UkbObservation>) Converter.convert(
    //                dataRetrievalService.getUkbRenalReplacementBodyWeight(icuLocalCaseIds));

    // Body weight should be a unique parameter by case, but it isn't since any ward can have one.
    // For the further calculation its enough to get the first one, so this step will prevent
    // duplicates.
    List<CoreBaseDataItem> bodyWeightRecent =
        dataRetrievalService.getUkbRenalReplacementBodyWeight(filteredRecentCaseIds, CLAPP);
    renalReplacementModelParameterSetMap
        .get(BODY_WEIGHT)
        .addAll(
            getUniqueItemsByHisCaseId(
                renalReplacementModelParameterSetMap.get(BODY_WEIGHT), bodyWeightRecent));

    //        renalReplacementModelParameterSetMap.put(BODY_WEIGHT,
    //            dataRetrievalService.getUkbRenalReplacementBodyWeight(filteredCaseIds,
    //                PDMS_REPORTING_DB));
    processTimer.stopLoggingTime(bodyWeightRecent);

    if (benchMarkRun) {
      logDelta(
          BODY_WEIGHT,
          renalReplacementModelParameterSetMap.get(BODY_WEIGHT),
          bodyWeightPdms,
          bodyWeightRecent,
          false);
    }

    if (!benchMarkRun) {
      log.debug(
          "Size caseIds before filtering of the cases without body weight: {}",
          filteredCaseIds.size());
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>>
          finalRenalReplacementModelParameterSetMap = renalReplacementModelParameterSetMap;
      filteredCaseIds.removeAll(
          filteredCaseIds.parallelStream()
              .filter(
                  x ->
                      !finalRenalReplacementModelParameterSetMap.get(BODY_WEIGHT).stream()
                          .map(CoreBaseDataItem::hisCaseId)
                          .collect(Collectors.toSet())
                          .contains(x))
              .collect(Collectors.toSet()));
      log.debug(
          "Size caseIds after filtering of the cases without body weight: {}",
          filteredCaseIds.size());
      //        Set<String> caseIdsObservations = icuLocalCaseIds.parallelStream()
      //            .filter(x -> renalReplacementModelParameterSetMap.get(LACTATE).stream().map(
      //                CoreBaseDataItem::caseId).contains(x))
      //            .stream().map(x -> x.getEncounter().getIdentifier().getValue())
      //            .collect(Collectors.toSet());
    }

    if (benchMarkRun) {
      processTimer.startLoggingTime(ResourceType.Observation, "CVVH clapp ALL");
      // Retrieval of the body weight

      renalReplacementModelParameterSetMap.put(
          START_REPLACEMENT,
          dataRetrievalService.getUkbRenalReplacementStart(filteredCaseIds, CLAPP));
      processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(START_REPLACEMENT));
    }

    processTimer.startLoggingTime(ResourceType.Observation, "CVVH pdmsreporting");

    List<CoreBaseDataItem> renalReplacementsPdms =
        dataRetrievalService.getUkbRenalReplacementStart(
            filteredCaseIdsWithoutRecents, PDMS_REPORTING_DB);
    if (!renalReplacementModelParameterSetMap.containsKey(START_REPLACEMENT)) {
      renalReplacementModelParameterSetMap.put(START_REPLACEMENT, renalReplacementsPdms);
    } else {
      renalReplacementModelParameterSetMap.get(START_REPLACEMENT).addAll(renalReplacementsPdms);
    }
    processTimer.stopLoggingTime(renalReplacementsPdms);

    processTimer.startLoggingTime(ResourceType.Observation, "CVVH clapp REDUCED");
    // Retrieval of the body weight

    List<CoreBaseDataItem> renalReplacementsRecent =
        dataRetrievalService.getUkbRenalReplacementStart(filteredRecentCaseIds, CLAPP);
    renalReplacementModelParameterSetMap.get(START_REPLACEMENT).addAll(renalReplacementsRecent);
    processTimer.stopLoggingTime(renalReplacementsRecent);

    // Logging the delta
    if (benchMarkRun) {
      logDelta(
          START_REPLACEMENT,
          renalReplacementModelParameterSetMap.get(START_REPLACEMENT),
          renalReplacementsPdms,
          renalReplacementsRecent,
          true);
    }

    if (benchMarkRun) {
      // Retrieval of the urine output
      processTimer.startLoggingTime(ResourceType.Observation, "urineOutput CLAPP ALL");
      //        List<UkbObservation> urineOutputResources =
      //            (List<UkbObservation>) Converter.convert(
      //                dataRetrievalService.getUkbRenalReplacementUrineOutput(icuLocalCaseIds));
      renalReplacementModelParameterSetMap.put(
          URINE_OUTPUT,
          dataRetrievalService.getUkbRenalReplacementUrineOutput(filteredCaseIds, CLAPP));
      processTimer.stopLoggingTime(renalReplacementModelParameterSetMap.get(URINE_OUTPUT));
    }

    processTimer.startLoggingTime(ResourceType.Observation, "urineOutput pdmsreporting");

    List<CoreBaseDataItem> urineOutputPdms =
        dataRetrievalService.getUkbRenalReplacementUrineOutput(
            filteredCaseIdsWithoutRecents, PDMS_REPORTING_DB);
    if (!renalReplacementModelParameterSetMap.containsKey(URINE_OUTPUT)) {
      renalReplacementModelParameterSetMap.put(URINE_OUTPUT, urineOutputPdms);
    } else {
      renalReplacementModelParameterSetMap.get(URINE_OUTPUT).addAll(urineOutputPdms);
    }
    processTimer.stopLoggingTime(urineOutputPdms);

    processTimer.startLoggingTime(ResourceType.Observation, "urineOutput CLAPP REDUCED");
    //        List<UkbObservation> urineOutputResources =
    //            (List<UkbObservation>) Converter.convert(
    //                dataRetrievalService.getUkbRenalReplacementUrineOutput(icuLocalCaseIds));
    //        renalReplacementModelParameterSetMap.put(URINE_OUTPUT,
    //            dataRetrievalService.getUkbRenalReplacementUrineOutput(filteredRecentCaseIds,
    // CLAPP));
    List<CoreBaseDataItem> urineOutputRecent =
        dataRetrievalService.getUkbRenalReplacementUrineOutput(filteredRecentCaseIds, CLAPP);
    renalReplacementModelParameterSetMap.get(URINE_OUTPUT).addAll(urineOutputRecent);
    processTimer.stopLoggingTime(urineOutputRecent);

    if (benchMarkRun) {
      logDelta(
          URINE_OUTPUT,
          renalReplacementModelParameterSetMap.get(URINE_OUTPUT),
          urineOutputPdms,
          urineOutputRecent,
          true);
    }
    return renalReplacementModelParameterSetMap;
  }

  private static Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> removeNonIcuLabValues(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>>
          renalReplacementModelParameterSetMap) {
    Instant timerStart = TimerTools.startTimer();
    // Check for each case whet
    List<CoreBaseDataItem> icuEpisodes = renalReplacementModelParameterSetMap.get(EPISODES);

    Map<String, List<Interval>> mapCaseIdEpisodes = new HashMap<>();
    for (CoreBaseDataItem item : icuEpisodes) {
      String hisCaseId = item.hisCaseId();
      Date dateFrom = item.dateFrom();
      // If the upper date is null it's an ongoing episode
      Date dateTo = item.dateTo() != null ? item.dateTo() : DateTools.getCurrentDateTime();
      Interval interval = new Interval(dateFrom, dateTo);

      mapCaseIdEpisodes.compute(
          hisCaseId,
          (key, existingIntervals) -> {
            if (existingIntervals == null) {
              List<Interval> intervals = new ArrayList<>();
              intervals.add(interval);
              return intervals;
            } else {
              existingIntervals.add(interval);
              return existingIntervals;
            }
          });
    }
    renalReplacementModelParameterSetMap.put(
        CREATININE,
        createMapWithIntersectionsOnly(
            mapCaseIdEpisodes,
            renalReplacementModelParameterSetMap.get(CREATININE),
            CREATININE.name()));
    renalReplacementModelParameterSetMap.put(
        LACTATE,
        createMapWithIntersectionsOnly(
            mapCaseIdEpisodes, renalReplacementModelParameterSetMap.get(LACTATE), LACTATE.name()));
    renalReplacementModelParameterSetMap.put(
        UREA,
        createMapWithIntersectionsOnly(
            mapCaseIdEpisodes, renalReplacementModelParameterSetMap.get(UREA), UREA.name()));

    TimerTools.stopTimerAndLog(timerStart, "Filtering lab values due episodes intersections");
    return renalReplacementModelParameterSetMap;
  }

  /**
   * Creation of a list of core base data items for a given context that MUST HAVE an intersection
   * with an icu episode.
   */
  private static List<CoreBaseDataItem> createMapWithIntersectionsOnly(
      Map<String, List<Interval>> map, List<CoreBaseDataItem> inputEntries, String context) {
    List<CoreBaseDataItem> filteredValues = new ArrayList<>();

    log.debug(
        context
            + ": Size of entries before filtering to intersections only: "
            + inputEntries.size());
    for (CoreBaseDataItem labValue : inputEntries) {
      String hisCaseId = labValue.hisCaseId();
      Date labValueDateFrom = labValue.dateFrom();

      if (map.containsKey(hisCaseId)) {
        List<Interval> episodeIntervals = map.get(hisCaseId);
        for (Interval episodeInterval : episodeIntervals) {
          Date episodeDateFrom = episodeInterval.startDate();
          Date episodeDateTo = episodeInterval.endDate();
          // Find intersections
          if (labValueDateFrom.before(episodeDateTo) && labValueDateFrom.after(episodeDateFrom)) {
            filteredValues.add(labValue);
            break; // Exit the inner loop since an intersection is found
          }
        }
      }
    }

    log.debug(
        "{}: Size of entries after filtering to intersections only: {}",
        context,
        filteredValues.size());

    // Temporary output to generate samples of filtered cases
    Set<String> filteredHisCaseIds =
        filteredValues.stream().map(CoreBaseDataItem::hisCaseId).collect(Collectors.toSet());
    inputEntries.stream()
        .map(CoreBaseDataItem::hisCaseId)
        .filter(item -> !filteredHisCaseIds.contains(item))
        .limit(10)
        .forEach(item -> log.debug("No valid {} found for {}", context, item));

    return filteredValues;
  }

  /**
   * Some parameters as the body weight should be a unique parameter by case, but it isn't since any
   * ward can have one. For the further calculation its enough to get the first one, so this method
   * will prevent duplicates by checking if a {@link CoreBaseDataItem} with the same his case id is
   * already existing in the list.
   *
   * @param oldCoreBaseDataItems A collection of {@link CoreBaseDataItem items} that are going to be
   *     checked against.
   * @param newCoreBaseDataItems The new collection of {@link CoreBaseDataItem items} of which the
   *     items could be added to the previous one.
   * @return A list of items that are not already part of the given list.
   */
  private static List<CoreBaseDataItem> getUniqueItemsByHisCaseId(
      List<CoreBaseDataItem> oldCoreBaseDataItems, List<CoreBaseDataItem> newCoreBaseDataItems) {
    Set<String> hisCaseIds =
        oldCoreBaseDataItems.stream().map(CoreBaseDataItem::hisCaseId).collect(Collectors.toSet());
    return newCoreBaseDataItems.stream()
        .filter(item -> !hisCaseIds.contains(item.hisCaseId()))
        .toList();
  }

  /**
   * This is a method only necessary in Bonn which compares the cardinality of data from 2 actually
   * identical ICU-PDMS databases and logs deviations.
   *
   * @param renalReplacementRiskParameter The context of the delta check.
   * @param clappAll CLAPP data from all cases considered, not just current cases.
   * @param pdmsData The data from the PDMS ReportingDB.
   * @param clappRecentCases CLAPP data from current or young cases.
   * @param resourcesComparable Is the parameter to be tested present in both systems and can be
   *     compared accordingly?
   */
  private static void logDelta(
      RenalReplacementRiskParameters renalReplacementRiskParameter,
      List<CoreBaseDataItem> clappAll,
      List<CoreBaseDataItem> pdmsData,
      List<CoreBaseDataItem> clappRecentCases,
      boolean resourcesComparable) {

    // Some outputs are just useful if the "recent" logic is not in use like comparing CLAPP-ALL
    // with PDMS data
    boolean noRecentUsage = clappRecentCases == null || clappRecentCases.isEmpty();

    // Fill a list to get case id information once
    Map<String, String> resourceIdsByCaseId = new ConcurrentHashMap<>();
    clappAll.parallelStream()
        .forEach(item -> resourceIdsByCaseId.put(item.debugKey(), item.hisCaseId()));
    log.debug("Size resourceIdsByCaseId resources : {}", resourceIdsByCaseId.size());

    pdmsData.parallelStream()
        .forEach(item -> resourceIdsByCaseId.putIfAbsent(item.debugKey(), item.hisCaseId()));
    log.debug("Size resourceIdsByCaseId resources after pdmsData: {}", resourceIdsByCaseId.size());

    if (!noRecentUsage) {
      clappRecentCases.parallelStream()
          .forEach(item -> resourceIdsByCaseId.putIfAbsent(item.debugKey(), item.hisCaseId()));
      log.debug(
          "Size resourceIdsByCaseId resources after clappRecent: {}", resourceIdsByCaseId.size());
    }

    Set<String> caseIdsClappAllByType =
        clappAll.parallelStream().map(CoreBaseDataItem::hisCaseId).collect(Collectors.toSet());
    Set<String> caseIdsPdmsByType =
        pdmsData.parallelStream().map(CoreBaseDataItem::hisCaseId).collect(Collectors.toSet());

    Set<String> caseIdsNotInBoth = new HashSet<>(caseIdsClappAllByType);
    caseIdsNotInBoth.removeAll(caseIdsPdmsByType);

    Set<String> resourceIdsClappAllByType =
        clappAll.parallelStream().map(CoreBaseDataItem::debugKey).collect(Collectors.toSet());
    Set<String> resourceIdsPdmsByType =
        pdmsData.parallelStream().map(CoreBaseDataItem::debugKey).collect(Collectors.toSet());

    Set<String> resourceIdsNotInBoth = new HashSet<>(resourceIdsClappAllByType);
    resourceIdsNotInBoth.removeAll(resourceIdsPdmsByType);
    if (noRecentUsage && resourcesComparable) {
      for (String resourceId : resourceIdsNotInBoth) {
        log.debug(
            "{} [ResourceID]  {} is just in CLAPP not in ReportingDB [CASE= {}]",
            resourceId,
            renalReplacementRiskParameter,
            resourceIdsByCaseId.get(resourceId));
      }
    }

    log.debug(
        "Delta [ResourceIds] in {} between CLAPP-ALL and REPORTING DB is: {}",
        renalReplacementRiskParameter,
        resourceIdsNotInBoth.size());
    log.debug(
        "Delta [CaseIds] in {} between CLAPP-ALL and REPORTING DB is: {}",
        renalReplacementRiskParameter,
        caseIdsNotInBoth.size());

    Set<String> resourceIdsClappAllByTypeList =
        clappAll.parallelStream().map(CoreBaseDataItem::debugKey).collect(Collectors.toSet());
    Set<String> resourceIdsPdmsByTypeList =
        pdmsData.parallelStream().map(CoreBaseDataItem::debugKey).collect(Collectors.toSet());

    if (resourcesComparable) {
      Set<String> resourceIdsNotInBothByType = new HashSet<>(resourceIdsClappAllByTypeList);
      resourceIdsNotInBothByType.removeAll(resourceIdsPdmsByTypeList);
      if (noRecentUsage) {
        for (String resourceId : resourceIdsNotInBothByType) {
          log.debug(
              resourceId
                  + " [resourceId] "
                  + renalReplacementRiskParameter
                  + " for case "
                  + resourceIdsByCaseId.get(resourceId)
                  + " is just in CLAPP not in ReportingDB ");
        }
      }
    }

    Set<String> caseIdsPdmsAndClappRecentByType = new HashSet<>(caseIdsPdmsByType);
    if (clappRecentCases != null) {
      caseIdsPdmsAndClappRecentByType.addAll(
          clappRecentCases.parallelStream()
              .map(CoreBaseDataItem::hisCaseId)
              .collect(Collectors.toSet()));
    }
    Set<String> caseIdsNotInBothByTypePlusRecents = new HashSet<>(caseIdsPdmsAndClappRecentByType);
    caseIdsClappAllByType.removeAll(caseIdsNotInBothByTypePlusRecents);
    log.debug(
        "{}: Number of cases that are not in reporting db OR retrievable via the recent clapp: {}",
        renalReplacementRiskParameter,
        caseIdsClappAllByType.size());

    caseIdsClappAllByType.forEach(
        x ->
            log.debug(
                "{} {} [CaseId] is just in CLAPP(ALL) not in ReportingDB or recent CLAPP",
                x,
                renalReplacementRiskParameter));

    // RESOURCE LEVEL
    if (resourcesComparable) {
      Set<String> resourceIdsPdmsAndClappRecentByType = new HashSet<>(resourceIdsPdmsByType);
      if (clappRecentCases != null) {
        resourceIdsPdmsAndClappRecentByType.addAll(
            clappRecentCases.parallelStream()
                .map(CoreBaseDataItem::debugKey)
                .collect(Collectors.toSet()));
      }
      Set<String> resourceIdsNotInBothByTypePlusRecents =
          new HashSet<>(resourceIdsPdmsAndClappRecentByType);
      Set<String> resourceIdsClappAllByTypeClone = new HashSet<>(resourceIdsClappAllByType);
      resourceIdsClappAllByTypeClone.removeAll(resourceIdsNotInBothByTypePlusRecents);
      log.debug(
          "{}: Number of RESOURCES that are not in reporting db OR retrievable via the recent "
              + "clapp: {}",
          renalReplacementRiskParameter,
          resourceIdsClappAllByTypeClone.size());

      resourceIdsClappAllByTypeClone.forEach(
          resourceId ->
              log.debug(
                  resourceId
                      + " "
                      + renalReplacementRiskParameter
                      + " [Resource] is just in CLAPP(ALL) not in ReportingDB or recent CLAPP."
                      + " CaseId: "
                      + resourceIdsByCaseId.get(resourceId)));

      log.debug(
          "Delta [ResourceIds] in {} between CLAPP-ALL [{}] and REPORTING DB [{}] + CLAPP-RECENT "
              + "+ [{}] is: {}",
          renalReplacementRiskParameter,
          clappAll.size(),
          pdmsData.size(),
          clappRecentCases.size(),
          clappAll.size() - pdmsData.size() - clappRecentCases.size());

      // Vice versa. Entries in reporting db + clapp recent that are not in clapp all
      Set<String> resourceIdsNotInClappAll = new HashSet<>(resourceIdsClappAllByType);
      resourceIdsPdmsAndClappRecentByType.removeAll(resourceIdsNotInClappAll);
      resourceIdsPdmsAndClappRecentByType.forEach(
          resourceId ->
              log.debug(
                  "{} {} [Resource] is just in ReportingDB or recent CLAPP but not in CLAPP(ALL). "
                      + "CaseId: {}",
                  resourceId,
                  renalReplacementRiskParameter,
                  resourceIdsByCaseId.get(resourceId)));
    }
  }

  private static Set<String> getCaseIdsWithAllLabValues(
      Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>>
          renalReplacementModelParameterSetMap) {

    // reducing the input lists for any lab value to the case id
    Set<String> caseIdsWithLabValues =
        renalReplacementModelParameterSetMap.get(LACTATE).parallelStream()
            .map(CoreBaseDataItem::hisCaseId)
            .collect(Collectors.toSet());
    caseIdsWithLabValues.retainAll(
        renalReplacementModelParameterSetMap.get(CREATININE).parallelStream()
            .map(CoreBaseDataItem::hisCaseId)
            .collect(Collectors.toSet()));
    caseIdsWithLabValues.retainAll(
        renalReplacementModelParameterSetMap.get(UREA).parallelStream()
            .map(CoreBaseDataItem::hisCaseId)
            .collect(Collectors.toSet()));

    return caseIdsWithLabValues;
  }
}
