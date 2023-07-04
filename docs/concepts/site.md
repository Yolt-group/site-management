# Site

A site is a financial institution, usually a bank, that supplies us with financial data about users.
We can pull data from a site, but, some sites push data to us too.

Sites can be subcategorized in two groups:

- sites for scraping providers
- sites for direct connections

These terms aren't used consistently throughout the code base (or even colloquially).
We sometimes call a **scraping provider** a 'form' provider.
We sometimes call a **direct connection** provider a 'url' provider.
The reason for this is that historically a user had to fill in a form (with credentials) to add a 'scraping' provider.
A direct connection site can be added by providing consent to a bank by visiting a url.

The preferred nomenclature is: 'scraper', and 'direct connection'.


## Lifecycle

The sites that we support are a static collection that we control.
See [working with sites](#working-with-sites) for more information.


## Properties

This chapter contains information about the most important properties of a site.

**connectionType** `LoginType` (actually called ~~**loginType**~~ in `SiteEntity`)

Either `FORM` or `URL`.
This is legacy nomenclature.
`FORM` is used for 'scraper connections', because a user typically fills in a form to authenticate.
`URL` for 'direct connections', because a user typically authenticates via the oAuth2 flow that includes a redirect url.

**whiteListedAccountTypes** `List<AccountType>`

A site has a list of account types that it supports.
This list is a subset of the `enum` values defined in `nl.ing.lovebird.providerdomain.AccountType`.

This field serves two main purposes:

- it is an indication of what account types you can expect as a [user](user.md)
- it is used to filter the data returned by a [provider](provider.md), any account with a type not in this list will not be sent downstream to `ingestion`

**groupingBy** `String`

This is a somewhat unusual parameter, if different sites share this value, it means the sites are 'the same financial institution'.
See [Relations](#relations) for more information.

**noLongerSupported** `boolean`

A flag that indicates if a site is 'supported'.
When set to true it is no longer possible for users to add this site.

## Relations

A [site](site.md) and a [provider](provider.md) are closely related.
Please see [provider, site, and financial institution](../relations/provider-site-financial-institution.md) for more on this topic. 

## Working with sites

Please follow the process [adding a site](../working-with/how-to-add-site.md) if you want to add a new site.
That document contains the details.
