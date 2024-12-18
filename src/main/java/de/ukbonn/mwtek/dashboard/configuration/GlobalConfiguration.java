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

import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used to read out the global configuration of the dashboard processor.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Configuration
@ConfigurationProperties(prefix = "global")
@Getter
@Setter
public class GlobalConfiguration {

  /** Comma-separated list with input codes for FHIR Search and processing logic. */
  private Map<String, Object> inputCodes = new HashMap<>();

  /** Flag to add debug information (e.g. case ids / resource ids) to the output. */
  private Boolean debug = false;

  /**
   * The type of the server that is used for data retrieval whether its <i>FHIR</i> (any fhir
   * server) or <i>ACUWAVE</i>
   */
  private ServerTypeEnum serverType = ServerTypeEnum.FHIR;

  private Map<String, Boolean> predictionModels = new HashMap<>();

  private Boolean usePartOfInsteadOfIdentifier = false;

  /** Should the covid-19 data item generation take place? */
  private Boolean generateCovidData = true;

  /** Should the influenza data item generation take place? */
  private Boolean generateInfluenzaData = false;

  /** Should the kids radar data item generation take place? */
  private Boolean generateKidsRadarData = false;

  /**
   * Should the generation ukb-renal-replacement prediction model data item generation take place?
   * Currently not supported via FHIR server usage.
   */
  private Boolean generateUkbRenalReplacementModelData = false;

  /**
   * Instead of Encounter.location references, Encounters can be marked as icu-encounter via service
   * provider IDs
   */
  private Set<String> serviceProviderIdentifierOfIcuLocations = new HashSet<>();

  /**
   * Option to overwrite the assignment from the value set <a
   * href="https://simplifier.net/medizininformatikinitiative-modullabor/valuesetqualitativelaborergebnisse">here</a>.
   */
  private Map<String, List<String>> qualitativeLabCodes = new HashMap<>();
}
