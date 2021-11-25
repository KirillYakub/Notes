var elementsTextBox = [];
var elementsCoords = [];
var startCoords = { x: 0, y: 0 };
var currentCoords = { x: 0, y: 0 };
var distance = { x: 0, y: 0 };

var regular = /\d+/;
var move = false, text="";
var divElement = null, divID = null;
const mainDiv = document.querySelector(".mainArea");
const addButton = document.querySelector('.addElement');

const elementWidth = 150;
const elementHeight = 150;
const mainWidth = mainDiv.offsetWidth;
const mainHeight = mainDiv.offsetHeight;

if (!!localStorage.getItem('elements')) {
    elementsTextBox = JSON.parse(localStorage.getItem("text"));
    elementsCoords = JSON.parse(localStorage.getItem('elements'));
    addElement(elementsCoords);
}
function addElement(coords) {
    let element = '';
    for (let i = 0; i < coords.length; i++) {
        element += '<div class="square" id=' + String(i) + "div" + ' style="transform: translate(' + coords[i].x + 'px, ' + coords[i].y + 'px)">'
        element += '<input type="text" id=' + String(i) + "box" + ' class="textField"' + ' placeholder="Поле для ввода">'; 
        element += '<button id=' + String(i) + "button" + ' class="buttonField">Сохранить</button></div>';
    }
    mainDiv.innerHTML = element;
    console.log(mainDiv);
    for(let i = 0; i < coords.length; i++)
        document.getElementById(`${String(i) + "box"}`).value = elementsTextBox[i];
}
function pushText(){
    elementsTextBox.push(text);
}
function newCoords(coords) {
    divElement.style.cssText = 'transform: translate(' + coords.x + 'px, ' + coords.y + 'px)';
}

addButton.addEventListener('click', (e) => {
    if (!!elementsCoords.length) 
        elementsCoords.push({ x: 0, y: 0 });
    else
        elementsCoords = [{ x: 0, y: 0 }];
    pushText();
    addElement(elementsCoords);

    localStorage.setItem('text', JSON.stringify(elementsTextBox));
    localStorage.setItem('elements', JSON.stringify(elementsCoords));
});
mainDiv.addEventListener('click', function(e){
    if(!!e.target.classList.contains('buttonField')){
        elementsTextBox[e.target.id.match(regular)] = document.getElementById(`${String(e.target.id.match(regular)) + "box"}`).value;
        localStorage.setItem('text', JSON.stringify(elementsTextBox));
    }
});
mainDiv.addEventListener('mousedown', function (e) {
    if (!!e.target.classList.contains('square')) {
        move = true;
        divElement = e.target;
        startCoords.x = e.clientX;
        startCoords.y = e.clientY;
    }
});
mainDiv.addEventListener('mousemove', function (e) {
    if (move) {
        currentCoords.x = e.clientX;
        currentCoords.y = e.clientY;

        distance.x = elementsCoords[e.target.id.match(regular)].x + (currentCoords.x - startCoords.x);
        distance.y = elementsCoords[e.target.id.match(regular)].y + (currentCoords.y - startCoords.y);

        if (distance.x >= (mainWidth - elementWidth)) distance.x = mainWidth - elementWidth;
        if (distance.x <= 0) distance.x = 0;

        if (distance.y >= (mainHeight - elementHeight)) distance.y = mainHeight - elementHeight;
        if (distance.y <= 0) distance.y = 0;

        newCoords(distance);
    }
});
mainDiv.addEventListener('mouseup', function (e) {
    if (!!e.target.classList.contains('square')) {
        move = false;
        elementsCoords[e.target.id.match(regular)].x = distance.x;
        elementsCoords[e.target.id.match(regular)].y = distance.y;
        localStorage.setItem('elements', JSON.stringify(elementsCoords));
    }
});