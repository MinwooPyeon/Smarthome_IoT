import os
import sys
import subprocess

def main():
    idf_path = os.environ.get('IDF_PATH')
    if not idf_path:
        print("Error: IDF_PATH environment variable is not set")
        print("Please run 'export.sh' or 'export.bat' from your ESP-IDF installation")
        sys.exit(1)
    
    idf_script = os.path.join(idf_path, 'tools', 'idf.py')
    
    if not os.path.exists(idf_script):
        print(f"Error: ESP-IDF script not found at {idf_script}")
        print("Please check your ESP-IDF installation")
        sys.exit(1)
    
    try:
        cmd = [sys.executable, idf_script] + sys.argv[1:]
        subprocess.run(cmd, check=True)
    except subprocess.CalledProcessError as e:
        sys.exit(e.returncode)
    except KeyboardInterrupt:
        print("\nBuild interrupted by user")
        sys.exit(1)

if __name__ == '__main__':
    main()
