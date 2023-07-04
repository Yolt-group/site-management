# List sites

Listing the sites will show a [user](../concepts/user.md) the available [sites](../concepts/site.md).
The [client](../concepts/client.md) can request the available sites through the following endpoint: `GET /v2/sites` (`nl.ing.lovebird.sitemanagement.site.clientsite.ClientSiteController`).

This endpoint depends on the following information to determine if a site will be shown:

- [the available sites](../concepts/site.md): it all starts with the available sites (`select * from site_v2`)
- [user](../concepts/user.md): the user has influence because:
  - the user can be an experimental user
  - the user can be tagged with feature flags
- [client](../concepts/client.md): the client can have influence in several ways:
  - the [client-site](../concepts/client-site.md) has a property **tags** that can have influence on what is displayed

Clients can further filter the sites by providing the following optional query parameters:

- `tag` (a list no longer than 256)
- `redirectUrlId` (a `uuid`)

The system then performs these steps:

- The system performs a first filtering step based on the _type_ of a [site](../concepts/site.md). There are following types:

- The system performs a first filtering step, a site is included if:
  - if the site is a scraper and there is a [client-enabled-provider](../concepts/client-enabled-provider.md) for the [provider](../concepts/provider.md) of the site
  - if the site is a direct connection and there is a [client-redirect-url-enabled-provider](../concepts/client-redirect-url-enabled-provider.md) for the [provider](../concepts/provider.md) of the site\
    **unless** the query parameter `redirectUrlId` is present and no [client-redirect-url-enabled-provider](../concepts/client-redirect-url-enabled-provider.md) have a matching redirect url

- The system performs a second filtering step, a site is excluded if:
  - the site is not enabled in [client-enabled-provider](../concepts/client-enabled-provider.md)
  - the query parameter `tag` is present and the site is not tagged with *all* tags in the `tag` list
