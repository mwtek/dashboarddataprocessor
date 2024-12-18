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

import java.util.ArrayList;
import java.util.Arrays;
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
 * This class is used to parameterize the Search queries at the local {@link
 * de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum#ACUWAVE Acuwave} server of the data providers.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Configuration
@ConfigurationProperties(prefix = "acuwave.search")
@Getter
@Setter
public class AcuwaveSearchConfiguration extends SearchConfiguration {

  /** Orbis internal keys for laboratory codes. */
  private List<Integer> covidOrbisLabPcrCodes = new ArrayList<>();

  /** Orbis internal keys for laboratory codes that describe covid variants. */
  private List<Integer> covidOrbisLabVariantCodes = new ArrayList<>();

  /** batch size of the parallelized partial acuwave searches */
  private int batchSize = 200;

  /**
   * Orbis internal keys for parameter 1 in the ukb modell [Creatinine [Mass/volume] in Serum or
   * Plasma]
   */
  private Set<Integer> predictionModelUkbObservationOrbisCreatinineCodes =
      new HashSet<>(Arrays.asList(10143, 131, 132, 17342, 17360));

  private Set<Integer> predictionModelUkbObservationOrbisLactateCodes =
      new HashSet<>(Arrays.asList(189, 6037, 17197));

  private Set<Integer> predictionModelUkbObservationOrbisUreaCodes =
      new HashSet<>(Arrays.asList(133, 10142, 17343, 17361, 17437));

  /** Comma-separated list with input codes for FHIR Search and processing logic. */
  private Map<String, String> predictionModelUkbObservationOrbisCodes = new HashMap<>();

  /**
   * For debugging purposes. If this value is set, the data retrieval will not be done periodically,
   * but case by case.
   */
  private List<String> predictionModelUkbCaseIds = new ArrayList<>();

  /** For debugging purposes. The calendar years to query can be set this way. */
  private List<Integer> predictionModelYears = null;

  /** Orbis internal keys for influenza laboratory codes. */
  private List<Integer> influenzaOrbisLabPcrCodes = new ArrayList<>();
}
