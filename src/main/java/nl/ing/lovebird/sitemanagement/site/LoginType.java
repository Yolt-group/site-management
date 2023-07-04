package nl.ing.lovebird.sitemanagement.site;

/**
 * This class is a bit outdated. Previously we had scrapers, and direct connections.
 * Scrapers were doing things with forms, and direct connections something with urls.
 * With the introduction of 'dynamic flows' (i.e. a direct connection also utilizing forms to get information) this is not coupled
 * that way anymore.
 *
 * For now, you might want to read this as follows:
 * if site.loginType = 'FORM' it indicates that we use a scraper to setup a connection/user-site.
 * if site.loginType = 'URL' it indicates that there is no scraper in between. We have a dataProvider for this site that communicates with
 * the bank/site directly.
 */
public enum LoginType {

    FORM, URL

}
