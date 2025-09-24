#pragma once
#include <chrono>
#include "csv/csv_mapper.hpp"
#include "util.hpp"
#include "types.hpp"

namespace csv
{
    inline CsvMapper<Metrics> make_metrics_mapper()
    {
        CsvMapper<Metrics> m;
        m.add(make_column<Metrics>("ts", [](const Metrics &e)
                                   { return to_iso8601(e.ts); }, [](Metrics &e, const std::string &s)
                                   { e.ts = from_iso8601(s).value_or(std::chrono::system_clock::time_point{}); }));
        m.add(make_column<Metrics>("tAvg", [](const Metrics &e)
                                   { return std::to_string(e.tAvg); }, [](Metrics &e, const std::string &s)
                                   { e.tAvg = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("hAvg", [](const Metrics &e)
                                   { return std::to_string(e.hAvg); }, [](Metrics &e, const std::string &s)
                                   { e.hAvg = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("tEwma", [](const Metrics &e)
                                   { return std::to_string(e.tEwma); }, [](Metrics &e, const std::string &s)
                                   { e.tEwma = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("hEwma", [](const Metrics &e)
                                   { return std::to_string(e.hEwma); }, [](Metrics &e, const std::string &s)
                                   { e.hEwma = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("dewPoint", [](const Metrics &e)
                                   { return std::to_string(e.dewPoint); }, [](Metrics &e, const std::string &s)
                                   { e.dewPoint = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("heatIndex", [](const Metrics &e)
                                   { return std::to_string(e.heatIndex); }, [](Metrics &e, const std::string &s)
                                   { e.heatIndex = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("absHumidity", [](const Metrics &e)
                                   { return std::to_string(e.absHumidity); }, [](Metrics &e, const std::string &s)
                                   { e.absHumidity = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("wbgt", [](const Metrics &e)
                                   { return std::to_string(e.wbgt); }, [](Metrics &e, const std::string &s)
                                   { e.wbgt = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("pmv", [](const Metrics &e)
                                   { return std::to_string(e.pmv); }, [](Metrics &e, const std::string &s)
                                   { e.pmv = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("ppd", [](const Metrics &e)
                                   { return std::to_string(e.ppd); }, [](Metrics &e, const std::string &s)
                                   { e.ppd = to_f64(s).value_or(0.0); }));
        m.add(make_column<Metrics>("spike", [](const Metrics &e)
                                   { return e.spike ? "true" : "false"; }, [](Metrics &e, const std::string &s)
                                   { e.spike = to_bool(s).value_or(false); }));
        return m;
    }

    inline CsvMapper<IrSignalLog> make_log_mapper()
    {
        CsvMapper<IrSignalLog> m;
        m.add(make_column<IrSignalLog>("ts", [](const IrSignalLog &e)
                                       { return to_iso8601(e.ts); }, [](IrSignalLog &e, const std::string &s)
                                       { e.ts = from_iso8601(s).value_or(std::chrono::system_clock::time_point{}); }));
        m.add(make_column<IrSignalLog>("tx_id", [](const IrSignalLog &e)
                                       { return std::to_string(e.tx_id); }, [](IrSignalLog &e, const std::string &s)
                                       { e.tx_id = to_i64(s).value_or(0); }));
        m.add(make_column<IrSignalLog>("send_device_id", [](const IrSignalLog &e)
                                       { return e.send_device_id; }, [](IrSignalLog &e, const std::string &s)
                                       { e.send_device_id = s; }));
        m.add(make_column<IrSignalLog>("device_type", [](const IrSignalLog &e)
                                       { return e.device_type; }, [](IrSignalLog &e, const std::string &s)
                                       { e.device_type = s; }));
        m.add(make_column<IrSignalLog>("function", [](const IrSignalLog &e)
                                       { return e.function_label; }, [](IrSignalLog &e, const std::string &s)
                                       { e.function_label = s; }));
        m.add(make_column<IrSignalLog>("meta_data", [](const IrSignalLog &e)
                                       { return json_encode(e.meta_data); }, [](IrSignalLog &e, const std::string &s)
                                       { e.meta_data = json_parse_str_array(s); }));
        return m;
    }

    inline CsvMapper<IrSendDevice> make_device_mapper()
    {
        CsvMapper<IrSendDevice> m;
        m.add(make_column<IrSendDevice>())
    }
}
