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

package de.ukbonn.mwtek.dashboard.services;

import static de.ukbonn.mwtek.dashboard.misc.LoggingHelper.logParsingResourceException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.ukbonn.mwtek.dashboard.auth.RestConsumer;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for providing the {@link AcuwaveServerRestConfiguration} (e.g. which Loinc codes are to
 * be used) from <code>application.yaml</code>.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
@Service
public class AcuwaveSearchService extends RestConsumer implements SearchService {

  FhirContext ctx = FhirContext.forR4();

  protected AcuwaveServerRestConfiguration acuwaveServerConf;

  @Autowired
  public AcuwaveSearchService(AcuwaveServerRestConfiguration acuwaveServerConf) {
    super(acuwaveServerConf);
    this.acuwaveServerConf = acuwaveServerConf;
  }

  /**
   * Retrieve a FHIR search query and parse the result in FHIR {@link Bundle#getEntry() bundle entry
   * components} This method is used when pagination is not necessary.
   *
   * @param querySuffix The suffix with the Acuwave search logic to be appended to the FHIR server
   *     endpoint url (e.g. Kdslabor?code=1234).
   * @return The response from the FHIR search query, parsed into a FHIR {@link Bundle#getEntry()
   *     bundle entry components}
   */
  public List<Bundle.BundleEntryComponent> getBundleData(String querySuffix) {
    IParser parser = ctx.newJsonParser();
    String acuwaveServerEndpoint = this.acuwaveServerConf.getRestUrl();

    RestTemplate rest = this.getRestTemplate();
    String queryUrl = acuwaveServerEndpoint + querySuffix;
    log.info(queryUrl);
    try {
      ResponseEntity<String> searchRequest = rest.getForEntity(queryUrl, String.class);
      Bundle requestBundle = parser.parseResource(Bundle.class, searchRequest.getBody());
      return requestBundle.getEntry();
    } catch (Exception ex) {
      logParsingResourceException(ex, queryUrl);
    }
    return null;
  }

  @Override
  public Bundle getInitialBundle(String querySuffix) {
    return null;
  }

  @Override
  public Bundle getBundlePart(String linkToNextPart) {
    return null;
  }
}
