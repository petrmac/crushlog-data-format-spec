# Grades, the Difficulty Axis and Grading Scales

Added in CLDF 1.4.0. Every field described here is optional. An archive
written before 1.4.0 is a valid 1.4.0 archive.

## The difficulty axis

Every grade has an integer value on a difficulty axis. A higher value is a
harder grade. There are two axes:

- the **boulder axis**, shared by Font and V-scale
- the **route axis**, shared by French, YDS and UIAA

Values on the boulder axis and values on the route axis are not comparable
with each other.

The tables below are part of the format. Every reader and writer computes the
same value for the same grade. Grade names are case sensitive.

### Boulder axis

| Font | V-scale | Value |
|------|---------|-------|
| 4    | VB      | 0     |
| 4A   |         | 25    |
| 4B   |         | 50    |
| 4C   |         | 75    |
| 5A   | V0      | 100   |
| 5A+  |         | 150   |
| 5B   | V1      | 200   |
| 5B+  |         | 250   |
| 5C   | V2      | 300   |
| 5C+  |         | 350   |
| 6A   | V3      | 400   |
| 6A+  |         | 450   |
| 6B   | V4      | 500   |
| 6B+  |         | 550   |
| 6C   | V5      | 600   |
| 6C+  |         | 650   |
| 7A   | V6      | 700   |
| 7A+  | V7      | 800   |
| 7B   | V8      | 900   |
| 7B+  |         | 950   |
| 7C   | V9      | 1000  |
| 7C+  | V10     | 1100  |
| 8A   | V11     | 1200  |
| 8A+  | V12     | 1300  |
| 8B   | V13     | 1400  |
| 8B+  | V14     | 1500  |
| 8C   | V15     | 1600  |
| 8C+  | V16     | 1700  |
| 9A   | V17     | 1800  |

### Route axis

| French | YDS   | UIAA  | Value |
|--------|-------|-------|-------|
| 4a     | 5.6   | IV    | 0     |
| 4b     | 5.7   | IV+   | 100   |
| 4c     | 5.8   | V-    | 200   |
| 5a     | 5.9   | V     | 300   |
| 5b     | 5.10a | V+    | 400   |
| 5c     | 5.10b | VI-   | 500   |
| 6a     | 5.10c | VI    | 600   |
| 6a+    | 5.10d | VI+   | 700   |
| 6b     | 5.11a | VII-  | 800   |
| 6b+    | 5.11b | VII   | 900   |
| 6c     | 5.11c | VII+  | 1000  |
| 6c+    | 5.11d | VIII- | 1100  |
| 7a     | 5.12a | VIII  | 1200  |
| 7a+    | 5.12b | VIII+ | 1300  |
| 7b     | 5.12c | IX-   | 1400  |
| 7b+    | 5.12d | IX    | 1500  |
| 7c     | 5.13a | IX+   | 1600  |
| 7c+    | 5.13b | X-    | 1700  |
| 8a     | 5.13c | X     | 1800  |
| 8a+    | 5.13d | X+    | 1900  |
| 8b     | 5.14a | XI-   | 2000  |
| 8b+    | 5.14b | XI    | 2100  |
| 8c     | 5.14c | XI+   | 2200  |
| 8c+    | 5.14d | XII-  | 2300  |
| 9a     | 5.15a | XII   | 2400  |
| 9a+    | 5.15b | XII+  | 2500  |
| 9b     | 5.15c | XIII- | 2600  |
| 9b+    | 5.15d | XIII  | 2700  |
| 9c     | 5.16a | XIII+ | 2800  |
| 9c+    | 5.16b | XIV-  | 2900  |

### Rules

1. **Grade to value.** Look the grade up in the table of its system. A grade
   that is not in the table (for example `6A/B`, or a grade easier than the
   first row) has no value.
2. **Value to grade.** Take the grade of the wanted system whose value is
   nearest. When two grades are equally near, take the easier one. A value
   outside the table gives the nearest end of the table.
3. **`numeric` in a file.** A climb's `grades.numeric`, when present, is the
   climb's value. When it is absent, a reader computes the value by rule 1.
   A writer that sets `numeric` for a grade from the table must set the
   table's value.

## Fields on a climb's `grades`

| Field       | Type    | Meaning |
|-------------|---------|---------|
| `numeric`   | integer | The climb's value on the difficulty axis |
| `estimated` | boolean | `true` when the grade was derived from a level of a grading scale and not read off the climb |
| `scale`     | object  | The level the climb was logged on: `id` (the scale's id) and `level` (the level number, from 1) |

`system` and `grade` stay required and always hold a real grade. A climb
logged on a level carries the grade its level starts at, so a reader that
knows nothing about scales still shows a sensible grade.

```json
{
  "id": 12,
  "date": "2026-11-19",
  "type": "boulder",
  "finishType": "top",
  "grades": {
    "system": "font",
    "grade": "6A",
    "numeric": 400,
    "estimated": true,
    "scale": { "id": "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10", "level": 3 }
  }
}
```

## Grading scales on a location

Many gyms grade with numbered or coloured levels instead of Font grades. A
location lists its scales in `gradeScales`.

| Field       | Required | Meaning |
|-------------|----------|---------|
| `id`        | yes      | Globally unique id of the scale |
| `levels`    | yes      | The levels, easiest first |
| `updatedAt` | yes      | When the scale last changed |
| `climbType` | no       | `boulder` or `route`. Boulder when absent |
| `spread`    | no       | `even` or `widerTop`: how the levels were laid out when the scale was created. Informational |
| `deletedAt` | no       | Set when the scale was removed |

Each level:

| Field          | Required | Meaning |
|----------------|----------|---------|
| `level`        | yes      | Level number, from 1 (easiest) |
| `startGrade`   | yes      | The grade where the level starts: a Font grade for a boulder scale, a French grade for a route scale |
| `startNumeric` | yes      | The value of `startGrade` on the difficulty axis |
| `color`        | no       | Colour of the level, as a name or a hex value |

```json
{
  "id": 1,
  "name": "Boulder Hall",
  "isIndoor": true,
  "gradeScales": [
    {
      "id": "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10",
      "climbType": "boulder",
      "spread": "widerTop",
      "updatedAt": "2026-11-19T10:00:00.000Z",
      "levels": [
        { "level": 1, "startGrade": "4",  "startNumeric": 0 },
        { "level": 2, "startGrade": "5C", "startNumeric": 300, "color": "blue" },
        { "level": 3, "startGrade": "6A", "startNumeric": 400, "color": "red" }
      ]
    }
  ]
}
```

### Rules

1. **A level's value** is its `startNumeric`. A climb logged on level 3
   above has the value 400 and the grade `6A`.
2. **A level's range** runs from its start to one table row below the next
   level's start. The top level runs to the end of the table.
3. **Level starts** are grades from the axis table and strictly increase
   with the level number.
4. **A climb with `grades.scale`** refers to a scale listed in `gradeScales`
   of the location of the climb's session.
5. **A removed scale** stays in `gradeScales` with `deletedAt` set, so climbs
   logged on it still resolve. Their grade, level and estimate mark are kept.
6. **Two copies of the same scale** (same `id`): the one with the newer
   `updatedAt` wins.

## Compatibility

- The fields are optional and the schemas accept archives without them.
- A reader that ignores unknown fields reads a level climb as an ordinary
  climb with a Font grade.
- The Dart client ignores unknown fields in every version.
- The Java client rejected unknown fields up to and including 1.3.0. Use
  Java client 1.4.0 or later to read archives that carry these fields. From
  1.4.0 the Java reader ignores fields it does not know.
