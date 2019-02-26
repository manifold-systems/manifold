package manifold.api.host;

import abc.chrome;
import java.util.List;
import junit.framework.TestCase;

/**
 */
public class ChromeTest extends TestCase
{
  public void testChrome()
  {
    chrome chr = chrome.create(chrome.manifest_version._2, "foo", "1.0.0");
    chr.setBackground( chrome.background.create() );
    chrome.background background = chr.getBackground();
    chrome.action browser_action = chr.getBrowser_action();
    chrome.commands commands = chr.getCommands();
    chrome.content_scripts content_scripts = (chrome.content_scripts) chr.getContent_scripts();
    Object chrome_settings_overrides = chr.getChrome_settings_overrides();
    Object content_pack = chr.getContent_pack();
    String content_security_policy = chr.getContent_security_policy();
    Object current_locale = chr.getCurrent_locale();
    chrome.chrome_url_overrides chrome_url_overrides = chr.getChrome_url_overrides();
    String default_locale = chr.getDefault_locale();
    String description = chr.getDescription();
    String default_locale1 = chr.getDefault_locale();
    String devtools_page = chr.getDevtools_page();
    chrome.externally_connectable externally_connectable = chr.getExternally_connectable();
    chrome.file_browser_handlers file_browser_handlers = (chrome.file_browser_handlers) chr.getFile_browser_handlers();
    String homepage_url = chr.getHomepage_url();
    chrome.icons icons = chr.getIcons();
    Object anImport = chr.getImport();
    chrome.incognito incognito = chr.getIncognito();
    chrome.input_components input_components = (chrome.input_components) chr.getInput_components();
    String key = chr.getKey();
    chrome.manifest_version manifest_version = chr.getManifest_version();
    String minimum_chrome_version = chr.getMinimum_chrome_version();
    chrome.nacl_modules nacl_modules = (chrome.nacl_modules) chr.getNacl_modules();
    String name = chr.getName();
    chrome.oauth2 oauth2 = chr.getOauth2();
    Boolean offline_enabled = chr.getOffline_enabled();
    chrome.omnibox omnibox = chr.getOmnibox();
    List<String> optional_permissions = chr.getOptional_permissions();
    String options_page = chr.getOptions_page();
    chrome.options_ui options_ui = chr.getOptions_ui();
    chrome.action page_action = chr.getPage_action();
    List<String> permissions = chr.getPermissions();
    Object platforms = chr.getPlatforms();
    chrome.requirements requirements = chr.getRequirements();
    chrome.sandbox sandbox = chr.getSandbox();
    String short_name = chr.getShort_name();
    Object signature = chr.getSignature();
    Object spellcheck = chr.getSpellcheck();
    Object storage = chr.getStorage();
    Object system_indicator = chr.getSystem_indicator();
    chrome.tts_engine tts_engine = chr.getTts_engine();
    String update_url = chr.getUpdate_url();
    String version = chr.getVersion();
    String version_name = chr.getVersion_name();
    List<String> web_accessible_resources = chr.getWeb_accessible_resources();
  }

}
