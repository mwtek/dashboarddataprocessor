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
package de.ukbonn.mwtek.dashboard.misc;

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_AGGREGATED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_CHART_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_LIST;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemTypes.ITEMTYPE_STACKED_BAR_CHARTS;

import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThresholdCheck {

  /**
   * Filters a list of {@link DiseaseDataItem} based on threshold values. Removes items where any
   * value violates the specified threshold (i.e., values greater than 0 but below the threshold).
   *
   * @param dataItems The list of {@link DiseaseDataItem} objects to be filtered.
   * @param thresholds A map containing item names as keys and their respective threshold values as
   *     values.
   * @return A filtered list of {@link DiseaseDataItem} objects that do not violate any threshold
   *     constraints.
   */
  public static List<DiseaseDataItem> filterDataItemsByThreshold(
      List<DiseaseDataItem> dataItems, Map<String, Integer> thresholds) {

    // Create a copy of the list to safely modify it
    List<DiseaseDataItem> filteredDataItems = new ArrayList<>(dataItems);
    for (Map.Entry<String, Integer> entry : thresholds.entrySet()) {
      String dataItemName = entry.getKey();
      int threshold = entry.getValue();

      // Try to find the DiseaseDataItem by name
      DiseaseDataItem dataItem =
          dataItems.stream()
              .filter(item -> item.getItemname().equals(dataItemName))
              .findFirst()
              .orElse(null);

      // skipping if the dataItem was not found
      if (dataItem == null) {
        continue;
      }

      boolean needsToBeFiltered = checkThresholdViolation(dataItem, threshold);
      if (needsToBeFiltered) {
        filteredDataItems.remove(dataItem);
        logItemFiltering(dataItemName, threshold);
      }
    }
    return filteredDataItems;
  }

  /**
   * Checks whether any value in the given {@link DiseaseDataItem} violates the specified threshold.
   * A violation occurs if a value is greater than 0 but below the given threshold.
   *
   * <p>The behavior depends on the type of {@code dataItem}:
   *
   * <ul>
   *   <li>{@code AGGREGATED}: Checks if any value in the aggregated data map is below the
   *       threshold.
   *   <li>{@code LIST}: Processes lists, timestamped list pairs, or maps of lists to check for
   *       violations.
   *   <li>{@code STACKED_BAR_CHARTS}: Flattens nested list structures and checks for threshold
   *       violations.
   *   <li>{@code CHART_LIST}: Analyzes item occurrences in chart lists and verifies if any count is
   *       below the threshold.
   * </ul>
   *
   * @param dataItem The {@link DiseaseDataItem} containing the data to check.
   * @param threshold The threshold value against which the data is evaluated.
   * @return {@code true} if any value is greater than 0 and below the threshold, {@code false}
   *     otherwise.
   */
  @SuppressWarnings("unchecked")
  private static boolean checkThresholdViolation(DiseaseDataItem dataItem, int threshold) {
    Object rawData = dataItem.getData();
    if (rawData == null) {
      return false;
    }

    return switch (dataItem.getItemtype()) {
      case ITEMTYPE_AGGREGATED ->
          ((Map<String, Integer>) rawData)
              .values().stream().anyMatch(value -> value > 0 && value < threshold);
      case ITEMTYPE_LIST -> checkListThresholdViolation(rawData, threshold);
      case ITEMTYPE_STACKED_BAR_CHARTS ->
          ((StackedBarChartsItem) rawData)
              .getValues().stream()
                  .flatMap(List::stream)
                  .flatMap(List::stream)
                  .anyMatch(num -> num.doubleValue() > 0 && num.doubleValue() < threshold);
      case ITEMTYPE_CHART_LIST ->
          ((ChartListItem) rawData)
              .getValues().stream()
                  .anyMatch(
                      sublist ->
                          sublist.stream()
                              .collect(Collectors.groupingBy(item -> item, Collectors.counting()))
                              .values()
                              .stream()
                              .anyMatch(count -> count > 0 && count < threshold));

      default -> false;
    };
  }

  /**
   * Handles threshold violation checks for different list-based data structures.
   *
   * @param rawData The raw data, which could be a List, TimestampedListPair, or a Map of Lists.
   * @param threshold The threshold value to compare against.
   * @return true if any value in the structure is greater than 0 but below the threshold, false
   *     otherwise.
   */
  @SuppressWarnings("unchecked")
  private static boolean checkListThresholdViolation(Object rawData, int threshold) {
    if (rawData instanceof List<?> listData) {
      return listData.stream()
          .collect(Collectors.groupingBy(item -> item, Collectors.counting()))
          .values()
          .stream()
          .anyMatch(count -> count > 0 && count < threshold);
    } else if (rawData instanceof TimestampedListPair timestampedListPair) {
      return timestampedListPair.getValue().stream()
          .anyMatch(num -> num.doubleValue() > 0 && num.doubleValue() < threshold);
    } else if (rawData instanceof Map<?, ?> mapData) {
      return mapData.entrySet().stream()
          .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof List<?>)
          .map(entry -> (List<Long>) entry.getValue())
          .flatMap(List::stream)
          .anyMatch(num -> num > 0 && num < threshold);
    }
    return false;
  }

  private static void logItemFiltering(String dataItemName, int threshold) {
    log.info(
        "DataItem '{}' was filtered because at least one subitem did not meet the threshold [{}].",
        dataItemName,
        threshold);
  }
}
