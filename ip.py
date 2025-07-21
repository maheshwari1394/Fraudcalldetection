from flask import Flask, render_template, request
import requests

app = Flask(__name__)

MODEL_SERVER_URL = "http://192.168.137.X:5001/predict"  

@app.route('/', methods=['GET', 'POST'])
def index():
    prediction = None
    text = ""

    if request.method == 'POST':
        text = request.form['input_text']
        try:
            response = requests.post(MODEL_SERVER_URL, json={"text": text})
            prediction = response.json().get("prediction", "No response")
        except Exception as e:
            prediction = f"Error connecting to model server: {e}"

    return render_template('index.html', prediction=prediction, input_text=text)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
