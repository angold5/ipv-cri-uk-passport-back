package uk.gov.di.ipv.cri.passport.acceptance_tests.pages.PagesFromIpvCoreRepo.Archive;

import org.openqa.selenium.support.PageFactory;
import uk.gov.di.ipv.cri.passport.acceptance_tests.utilities.Driver;

public class DiIpvCoreFrontPage {

    public DiIpvCoreFrontPage() {
        PageFactory.initElements(Driver.get(), this);
    }
}
