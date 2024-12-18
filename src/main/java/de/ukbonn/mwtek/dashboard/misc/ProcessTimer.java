/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboard.misc;

import java.util.Arrays;
import java.util.Collection;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * Utility class for measuring the time taken to execute a specific task or to retrieve a collection
 * of FHIR resources.
 */
@Slf4j
@NoArgsConstructor
public class ProcessTimer {

  // Stores the ResourceType for FHIR resource retrieval tasks
  private ResourceType fhirResourceType;

  // Stores the description of the task being timed
  private String taskDescription;

  // Stores the start time of the process in milliseconds
  private long startTimeProcess;

  /**
   * Logs the time taken to retrieve a collection of FHIR resources.
   *
   * @param listResources A collection of FHIR resources that have been retrieved.
   */
  public void stopLoggingTime(Collection<?> listResources) {
    log.info(
        "Loading {}s took {} milliseconds for {} resources",
        fhirResourceType.name(),
        System.currentTimeMillis() - startTimeProcess,
        listResources.size());
  }

  /**
   * Starts logging the time taken for retrieving a collection of FHIR resources.
   *
   * @param fhirResourceType The type of FHIR resources being retrieved.
   * @param additions Optional additional information to be logged.
   */
  public void startLoggingTime(ResourceType fhirResourceType, String... additions) {
    this.fhirResourceType = fhirResourceType;
    startTimeProcess = System.currentTimeMillis();
    log.info(
        "Retrieval of the {} resources started{}",
        fhirResourceType.name(),
        additions.length > 0 ? " " + Arrays.toString(additions) : "");
  }

  /**
   * Starts logging the time taken for a specific task.
   *
   * @param taskDescription A description of the task being timed.
   */
  public void startLoggingTime(String taskDescription) {
    this.taskDescription = taskDescription;
    startTimeProcess = System.currentTimeMillis();
    log.info("{} started", taskDescription);
  }

  /** Stops logging the time taken for a specific task and logs the elapsed time. */
  public void stopLoggingTime() {
    if (taskDescription == null) {
      throw new IllegalStateException("taskDescription not set");
    }
    log.info(
        "{} took {} milliseconds.", taskDescription, System.currentTimeMillis() - startTimeProcess);
  }
}
