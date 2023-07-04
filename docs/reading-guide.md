# Reading guide

This folder, `docs/`, contains documentation about the `site-management` service.
It covers a whole host of topics because `site-management` is a complex beast that touches many different topics and concepts in Yolt.

There are several subfolders, respectively:

- `concepts/` describes all the concepts central to `site-management`.
- `ideas/` ideas and designs that are **not(()) (yet) implemented.
- `functions/` describes the 'information functions'.
  An information function is something that is useful to a client (can be an internal client).
  Think of a single endpoint, or (more commonly) a set of endpoints called in sequence to perform a function.
- `processes/` describes processes.
  This includes things that happen 'behind the scenes' and require no client interaction.
  Think of the flywheel (triggered by a cron job) or kafka consumers that result in some administration.
- `relations/` contains extra information on relations between `concepts/` if so required to clarify things
- `working-with/` describes 'how to do' something, such as adding a site, migrating a site, etc.
  This contains things that we ourselves have to do.
  These are manuals basically.

**todo** would it be a good idea to add a `kafka/` folder that describes our interaction with topics?\
**todo** would it be a good idea to add a `cassandra/` folder that describes the tables (1 file/table)?

## Contributing

Yes!
Please!
Read more about [contributing](contributing.md) here.