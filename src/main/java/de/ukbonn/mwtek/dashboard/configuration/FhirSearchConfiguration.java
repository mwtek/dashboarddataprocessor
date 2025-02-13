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

package de.ukbonn.mwtek.dashboard.configuration;

import de.ukbonn.mwtek.utilities.enums.TerminologySystems;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * This class is used to parameterize the FHIR Search queries at the local {@link
 * de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum#FHIR FHIR} server of the data providers.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Configuration
@ConfigurationProperties(prefix = "fhir.search")
@Getter
@Setter
public class FhirSearchConfiguration extends SearchConfiguration {

  /** batch size of the parallelized partial FHIR searches */
  private int batchSize = 500;

  /**
   * Does a reference exist from {@link org.hl7.fhir.r4.model.Condition} resource to {@link
   * org.hl7.fhir.r4.model.Encounter} via {@link Condition#getEncounter()} or only the reverse way
   * via {@link Encounter#getDiagnosis()}? This results in differences in the FHIR search queries.
   */
  private boolean useEncounterConditionReference = false;

  /**
   * This field allows you to handle "431 Request Header Fields Too Large" exceptions since it sends
   * {@link org.springframework.http.HttpMethod#POST} requests on the initial bundle calls instead
   * of {@link org.springframework.http.HttpMethod#GET} operations, that are limited in the number
   * of characters.
   *
   * <p>After an initial bundle was received and a paging is the result, it will continue with get
   * operations though, since the long url is not a problem anymore.
   *
   * <p>Default is <code>true</code>, since the procedure calls produce really long URLs by default
   * if you don't reduce the number of codes.
   */
  private boolean usePostInsteadOfGet = true;

  /**
   * This field allows you to handle "431 Request Header Fields Too Large" exceptions since it sends
   * {@link org.springframework.http.HttpMethod#POST} requests on the initial bundle calls instead
   * of {@link org.springframework.http.HttpMethod#GET} operations, that are limited in the number
   * of characters.
   *
   * <p>After an initial bundle was received and a paging is the result, it will continue with get
   * operations though, since the long url is not a problem anymore.
   */
  public HttpMethod getHttpMethod() {
    return usePostInsteadOfGet ? HttpMethod.POST : HttpMethod.GET;
  }

  /**
   * The system url of the ecmo/ventilation codes of the procedure resources. It is set as a prefix
   * in the FHIR search query before the codes, as it must be set in the Blaze Server queries to
   * find the perfect index, which can significantly reduce query times. In the case of proprietary
   * codes, this value must be overwritten with the local identifiers.
   *
   * <p>By default, the snomed system url is used.
   */
  private String procedureCodesSystemUrl = TerminologySystems.SNOMED;
}
