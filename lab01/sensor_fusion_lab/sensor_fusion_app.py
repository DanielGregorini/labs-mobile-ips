#!/usr/bin/env python3
import os
import re
import math
import argparse
import numpy as np
import pandas as pd

# evita problemas de GUI em alguns ambientes
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DATA_DIR = os.path.join(SCRIPT_DIR, "data_collection")
DEFAULT_OUT_DIR  = os.path.join(SCRIPT_DIR, "results")

def find_timestamp_col(df: pd.DataFrame):
    cands = [c for c in df.columns if re.search(r"(time|timestamp|epoch)", str(c), re.I)]
    return cands[0] if cands else None

def parse_timestamp_series(s: pd.Series) -> pd.Series:
    """Converte timestamp para pandas datetime; detecta s/ms/ns se numérico."""
    if pd.api.types.is_numeric_dtype(s):
        vals = pd.to_numeric(s, errors="coerce").dropna()
        m = float(vals.median()) if len(vals) else 0.0
        if m > 1e14:      # ns
            dt = pd.to_datetime(s, unit="ns", utc=True).dt.tz_localize(None)
        elif m > 1e11:    # ms
            dt = pd.to_datetime(s, unit="ms", utc=True).dt.tz_localize(None)
        else:             # s
            dt = pd.to_datetime(s, unit="s", utc=True).dt.tz_localize(None)
    else:
        dt = pd.to_datetime(s, errors="coerce")
    return dt

def parse_time_series(df: pd.DataFrame) -> pd.DataFrame:
    ts_col = find_timestamp_col(df)
    if ts_col is None:
        raise ValueError("Não encontrei coluna de timestamp neste CSV.")
    out = df.copy()
    out["timestamp"] = parse_timestamp_series(out[ts_col])
    out = out.dropna(subset=["timestamp"]).sort_values("timestamp")
    return out

def find_xyz_cols(df: pd.DataFrame):

    common_sets = [
        ("x","y","z"), ("ax","ay","az"), ("gx","gy","gz"),
        ("accel_x","accel_y","accel_z"), ("gyro_x","gyro_y","gyro_z"),
        ("linear_accel_x","linear_accel_y","linear_accel_z")
    ]
    cols = set(df.columns)
    for a,b,c in common_sets:
        if a in cols and b in cols and c in cols:
            return a,b,c

    # Fallback: 3 primeiras numéricas que NÃO parecem tempo
    def not_time(col):
        return re.search(r"(time|timestamp|epoch)", str(col), re.I) is None
    numeric = [c for c in df.columns
               if not_time(c) and pd.api.types.is_numeric_dtype(df[c])]
    if len(numeric) >= 3:
        return numeric[0], numeric[1], numeric[2]
    return None, None, None

def ensure_mps(speed_series: pd.Series) -> pd.Series:
    """Converte para m/s se parecer km/h."""
    s = pd.to_numeric(speed_series, errors="coerce")
    med = np.nanmedian(s)
    if np.isnan(med):
        return s
    if med > 30:  # km/h típico
        s = s / 3.6
    return s

#Ccrregamento Accelerometer, Gyroscope, Location 

def load_accelerometer(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    df = parse_time_series(df)
    ax, ay, az = find_xyz_cols(df)
    if not ax:
        raise RuntimeError("Não encontrei colunas x,y,z no Accelerometer.csv")
    df = df.rename(columns={ax: "accel_x", ay: "accel_y", az: "accel_z"})
    # magnitude & aceleração dinâmica (~remove gravidade)
    g = 9.80665
    df["accel_magnitude"] = np.sqrt(df["accel_x"]**2 + df["accel_y"]**2 + df["accel_z"]**2)
    df["dynamic_accel"]   = (df["accel_magnitude"] - g).abs()
    return df[["timestamp","accel_x","accel_y","accel_z","accel_magnitude","dynamic_accel"]]

def load_gyroscope(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    df = parse_time_series(df)
    gx, gy, gz = find_xyz_cols(df)
    if not gx:
        raise RuntimeError("Não encontrei colunas x,y,z no Gyroscope.csv")
    df = df.rename(columns={gx: "gyro_x", gy: "gyro_y", gz: "gyro_z"})
    df["gyro_magnitude"] = np.sqrt(df["gyro_x"]**2 + df["gyro_y"]**2 + df["gyro_z"]**2)
    return df[["timestamp","gyro_x","gyro_y","gyro_z","gyro_magnitude"]]

def load_location(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    df = parse_time_series(df)

    # tenta achar coluna de velocidade
    speed_col = None
    for pat in [r"speed.*m/?s", r"speed", r"velocity", r"km/?h", r"kph"]:
        for c in df.columns:
            if re.search(pat, str(c), re.I):
                speed_col = c
                break
        if speed_col: break

    if speed_col is not None:
        speed = ensure_mps(df[speed_col])
    else:
        # calcula a partir de lat/lon se existir
        lat_col = next((c for c in df.columns if re.search(r"lat", str(c), re.I)), None)
        lon_col = next((c for c in df.columns if re.search(r"(lon|lng)", str(c), re.I)), None)
        if lat_col and lon_col:
            R = 6371000.0
            lat = np.radians(pd.to_numeric(df[lat_col], errors="coerce"))
            lon = np.radians(pd.to_numeric(df[lon_col], errors="coerce"))
            dlat = lat.diff()
            dlon = lon.diff()
            a = np.sin(dlat/2)**2 + np.cos(lat).shift(1)*np.cos(lat)*np.sin(dlon/2)**2
            c = 2*np.arctan2(np.sqrt(a), np.sqrt(1-a))
            dist = R * c
            dt = df["timestamp"].diff().dt.total_seconds()
            speed = dist / dt
        else:
            speed = pd.Series(np.nan, index=df.index)

    out = pd.DataFrame({"timestamp": df["timestamp"], "speed": speed})
    return out.dropna()

def load_annotations(path: str) -> pd.DataFrame | None:
    if not os.path.exists(path):
        return None
    ann = pd.read_csv(path)
    try:
        ann = parse_time_series(ann)
    except Exception:
        # fallback: primeira coluna vira timestamp
        ann["timestamp"] = pd.to_datetime(ann.iloc[:,0], errors="coerce")
        ann = ann.dropna(subset=["timestamp"])
    label_col = next((c for c in ann.columns
                      if re.search(r"(label|note|annot|activity|state|mode)", str(c), re.I)), None)
    if label_col is None:
        label_col = ann.columns[-1]
    return ann[["timestamp", label_col]].rename(columns={label_col: "label"}).sort_values("timestamp")

def extract_features_window(acc_slice: pd.DataFrame,
                            gyr_slice: pd.DataFrame,
                            gps_slice: pd.DataFrame) -> dict:
    avg_speed = gps_slice["speed"].mean() if len(gps_slice) else 0.0
    accel_var = acc_slice["dynamic_accel"].var() if len(acc_slice) else 0.0
    gyro_var  = gyr_slice["gyro_magnitude"].var() if len(gyr_slice) else 0.0
    accel_max = acc_slice["dynamic_accel"].max() if len(acc_slice) else 0.0
    # torna float “puro” para facilitar export
    def f(x): 
        return float(x) if x == x and np.isfinite(x) else 0.0
    return {
        "avg_speed":      f(avg_speed),
        "accel_variance": f(accel_var),
        "gyro_variance":  f(gyro_var),
        "accel_max":      f(accel_max),
    }

def classify_rule_based(features: dict) -> str:
    v = features["avg_speed"]
    a = features["accel_variance"]
    if v < 0.5:
        return "Stationary"
    elif v < 2.5:
        return "Walking" if a > 0.3 else "Unknown"
    else:
        return "Driving" if a < 1.5 else "Unknown"

def sliding_windows(accel: pd.DataFrame, gyro: pd.DataFrame, gps: pd.DataFrame,
                    window_seconds: int, step_seconds: int) -> pd.DataFrame:
    tmin = max(accel["timestamp"].min(), gyro["timestamp"].min())
    tmax = min(accel["timestamp"].max(), gyro["timestamp"].max())
    results = []
    t = tmin
    while t < tmax:
        t2 = t + pd.Timedelta(seconds=window_seconds)
        a_slice = accel[(accel["timestamp"] >= t) & (accel["timestamp"] < t2)]
        g_slice = gyro[(gyro["timestamp"] >= t) & (gyro["timestamp"] < t2)]
        v_slice = gps[(gps["timestamp"] >= t) & (gps["timestamp"] < t2)]
        feats = extract_features_window(a_slice, g_slice, v_slice)
        mode = classify_rule_based(feats)
        results.append({"start": t, "end": t2, "predicted_mode": mode, **feats})
        t += pd.Timedelta(seconds=step_seconds)
    return pd.DataFrame(results)

# -------------- Visualização e export --------------

def make_plots(results_df: pd.DataFrame, out_dir: str):
    if not len(results_df):
        print("Sem resultados para plotar.")
        return
    os.makedirs(out_dir, exist_ok=True)

    plt.figure(figsize=(10,4))
    plt.plot(results_df["start"], results_df["avg_speed"])
    plt.title("Average speed per window (m/s)")
    plt.xlabel("Time")
    plt.ylabel("m/s")
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, "speed_windows.png"), dpi=200, bbox_inches="tight")
    plt.close()

    plt.figure(figsize=(10,4))
    plt.plot(results_df["start"], results_df["accel_variance"])
    plt.title("Accel variance per window")
    plt.xlabel("Time")
    plt.ylabel("variance")
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, "accel_var_windows.png"), dpi=200, bbox_inches="tight")
    plt.close()

    results_df["predicted_mode"].value_counts().to_csv(os.path.join(out_dir, "mode_counts.csv"))

def export_results(results_df: pd.DataFrame, annotations: pd.DataFrame | None, out_dir: str):
    os.makedirs(out_dir, exist_ok=True)
    results_df.to_csv(os.path.join(out_dir, "analysis_results_windows.csv"), index=False)
    if annotations is not None and len(annotations):
        labeled = results_df.copy()
        ann_series = annotations.set_index("timestamp").sort_index()["label"]
        labeled["label"] = ann_series.reindex(labeled["start"], method="ffill").values
        labeled.to_csv(os.path.join(out_dir, "analysis_with_labels.csv"), index=False)

def main():
    ap = argparse.ArgumentParser(description="Sensor Fusion (Stationary/Walking/Driving) para CSVs do app")
    ap.add_argument("--data-dir", default=DEFAULT_DATA_DIR)
    ap.add_argument("--out-dir",  default=DEFAULT_OUT_DIR)
    ap.add_argument("--window", type=int, default=30, help="tamanho da janela (s)")
    ap.add_argument("--step",   type=int, default=10, help="passo entre janelas (s)")
    args = ap.parse_args()

    acc_path = os.path.join(args.data_dir, "Accelerometer.csv")
    gyr_path = os.path.join(args.data_dir, "Gyroscope.csv")
    gps_path = os.path.join(args.data_dir, "Location.csv")
    ann_path = os.path.join(args.data_dir, "Annotation.csv")

    missing = [p for p in (acc_path, gyr_path, gps_path) if not os.path.exists(p)]
    if missing:
        print("ERRO: Não encontrei os arquivos obrigatórios abaixo:")
        for p in missing:
            print(" -", p)
        print("\nDica: seus CSVs precisam estar em:", args.data_dir)
        try:
            print("\nConteúdo da pasta:")
            for name in os.listdir(args.data_dir):
                print(" *", name)
        except Exception:
            pass
        raise SystemExit(1)

    print("Usando dados de:", args.data_dir)
    print("Resultados em  :", args.out_dir)

    print("Carregando CSVs...")
    accel = load_accelerometer(acc_path)
    gyro  = load_gyroscope(gyr_path)
    gps   = load_location(gps_path)
    ann   = load_annotations(ann_path)

    print("Extraindo janelas e classificando...")
    results = sliding_windows(accel, gyro, gps, args.window, args.step)

    print("Gerando gráficos e exportando CSVs...")
    make_plots(results, args.out_dir)
    export_results(results, ann, args.out_dir)

    print("\nConcluído.")
    print(f"- CSV: {os.path.join(args.out_dir, 'analysis_results_windows.csv')}")
    if ann is not None:
        print(f"- CSV (com rótulos): {os.path.join(args.out_dir, 'analysis_with_labels.csv')}")
    print(f"- Figuras: {os.path.join(args.out_dir, 'speed_windows.png')} e 'accel_var_windows.png'")
    print(f"- Contagem de modos: {os.path.join(args.out_dir, 'mode_counts.csv')}")

if __name__ == "__main__":
    main()
