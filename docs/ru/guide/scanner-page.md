---
sidebarDepth: 2
tags:
  - exodus
  - трекер
  - реклама
  - блокировка
  - библиотека
  - анти-функции
---

# Страница Сканера
**Страница Сканера** появляется после нажатия на кнопку _сканер_ на вкладке [Сведения о приложении][app_info]. External APK files can also be opened for scanning from file managers, web browsers, etc.

Сканируются трекеры и библиотеки, а также отображается количество трекеров и библиотек в виде сводки. Также отображаются контрольные суммы файла APK и подписывающего сертификата(ов).

::: danger Disclaimer
AM only scans an app statically. An app may provide the options for opting out, or in some cases, certain features of the tracker may not be used at all by the app (such as F-Droid), or some apps may simply use them as placeholders to prevent breaking certain features (such as Fennec F-Droid). The intention of the scanner is to give you an idea about what the APK might contain. It should be taken as an initial step for further investigations.
:::

Clicking on the first item (i.e. number of classes) opens a new page containing a list of tracker classes for the app. All classes can also be viewed by clicking on the _Toggle Class Listing_ menu. A sneak-peek of each class can be viewed by simply clicking on any class item.

::: warning Notice
Due to various limitations, it is not possible to scan all the components of an APK file. This is especially true if an APK is highly obfuscated. The scanner also does not check strings (or website signatures).
:::

The second item lists the number of trackers along with their names. Clicking on the item displays a dialog containing the name of trackers, matched signatures and the number of classes against each signature. The tracker names may have some prefixes such as:
- `°` denotes that the tracker is missing in the εxodus' list (taken from [IzzyOnDroid repo][izzy])
- `²` denotes that the tracker is in the [ETIP][etip] stand-by list i.e. whether it is an actual tracker is not yet decided
- `µ` denotes micro non-intrusive tracker meaning that these trackers are harmless but still a tracker
- `?` denotes that the tracker status is unknown

The third item lists the number of libraries along with their names. These information are taken from [IzzyOnDroid repo][izzy].

_See also: [FAQ: Tracker vs tracker components][t_vs_tc]_

[app_info]: ./app-details-page.md#вкладка-«о-приложении»
[etip]: https://etip.exodus-privacy.eu.org
[t_vs_tc]: ../faq/app-components.md#класс-трекера-и-компоненты-трекера-в-чем-разница
[izzy]: https://gitlab.com/IzzyOnDroid/repo