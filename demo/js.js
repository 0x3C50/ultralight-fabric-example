document.getElementById("bbb").addEventListener("click", function (ev) {
	let vl = document.getElementById("aaa").value;
	document.getElementById('ccc').innerHTML = "You put: " + vl;
});