# User site status

The field **status** on a [user-site](user-site.md) is used to keep track of the status of both:

1. [adding a site](../functions/add-site.md)
2. [fetching data](../functions/fetch-data.md)

These processes both change the **status** field.


## Adding a site

The transition diagram for adding a site is the following:

```
                      +---> LOGIN_FAILED
                      |
                      |
INITIAL_PROCESSING ---+---> LOGIN_SUCCEEDED --+--> REFRESH_FINISHED
                      |                  ^    |
                      |        +----+    |    |
                      |        v    |    |    +--> REFRESH_TIMED_OUT
                      +---> STEP_NEEDED -+    |
                             +                +--> REFRESH_FAILED
                             |
                             +---> STEP_TIMED_OUT
                             |
                             +---> STEP_FAILED

                * -> UNKNOWN
```

**todo** check the accuracy

**Note**: in practice [adding a site](../functions/add-site.md) is always immediately followed by [fetching data](../functions/fetch-data.md).
This is why the terminal states are all related to [fetching data](../functions/fetch-data.md).
In principle the system could stop at `LOGIN_SUCCEEDED` if all we were interested in was adding the site.

## Fetching data

**todo** document this

## When things go wrong

**todo** say something about timeouts, etc.

