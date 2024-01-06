package com.example.pharmascan

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.pharmascan.GenerativeViewModelFactory
import coil.size.Precision
import com.example.pharmascan.util.UriSaver
import kotlinx.coroutines.launch


@Composable
internal fun ImageInterpretationRoute(
    viewModel: ImageInterpretationViewModel = viewModel(factory = GenerativeViewModelFactory)
) {
    val imageInterpretationUiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val imageRequestBuilder = ImageRequest.Builder(LocalContext.current)
    val imageLoader = ImageLoader.Builder(LocalContext.current).build()

    ImageInterpretationScreen(
        uiState = imageInterpretationUiState,
        onReasonClicked = { inputText, selectedItems ->
            coroutineScope.launch {
                val bitmaps = selectedItems.mapNotNull {
                    val imageRequest = imageRequestBuilder
                        .data(it)
                        // Scale the image down to 768px for faster uploads
                        .size(size = 768)
                        .precision(Precision.EXACT)
                        .build()
                    try {
                        val result = imageLoader.execute(imageRequest)
                        if (result is SuccessResult) {
                            return@mapNotNull (result.drawable as BitmapDrawable).bitmap
                        } else {
                            return@mapNotNull null
                        }
                    } catch (e: Exception) {
                        return@mapNotNull null
                    }
                }
                viewModel.reason(inputText, bitmaps)
            }
        }
    )
}

@Composable
fun ImageInterpretationScreen(
    uiState: ImageInterpretationUiState = ImageInterpretationUiState.Loading,
    onReasonClicked: (String, List<Uri>) -> Unit = { _, _ -> }
) {
    val textPrompt = "Get the name of the medicine, its symptoms, primary diagnosis, usage, and dosage from the input image in the following format. \n" +
            "Example: ● Name\n" +
            "● Symptoms: \n and so on." +
            "Make sure to ask the person to visit the doctor if problem persists."
    val imageUris = rememberSaveable(saver = UriSaver()) { mutableStateListOf() }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        imageUri?.let {
            imageUris.add(it)
        }
    }

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Button(
            onClick = {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .padding(all = 4.dp)
                .align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9100),
                contentColor = Color(0xFFFFFFFF))
        ) {

            Text("Upload Image")
        }

        LazyRow(
            modifier = Modifier.padding(all = 8.dp)
        ) {
            items(imageUris) { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .requiredSize(300.dp) // changed by me.
                )
            }
        }

        Button(
            onClick = {
                if (textPrompt.isNotBlank()) {
                    onReasonClicked(textPrompt, imageUris.toList())
                }
            },
            modifier = Modifier
                .padding(all = 4.dp)
                .align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9100),
                contentColor = Color(0xFFFAFAFA))
            //.align(Alignment.CenterVertically)
        ) {
            Text("Submit")

        }

        when (uiState) {
            ImageInterpretationUiState.Initial -> {
                // Nothing is shown
            }

            ImageInterpretationUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    CircularProgressIndicator(color = Color(0XFFFFE28C))
                }
            }

            is ImageInterpretationUiState.Success -> {
                Card(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0XFFFFE28C)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .fillMaxWidth()
                    ) {
                        /* Icon(
                            Icons.Outlined.Person,
                            contentDescription = "Person Icon",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .requiredSize(36.dp)
                                .drawBehind {
                                    drawCircle(color = Color.White)
                                }
                        )*/  // changed by me.
                        Text(
                            text = uiState.outputText, // TODO(thatfiredev): Figure out Markdown support
                            color = Color.Black,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            is ImageInterpretationUiState.Error -> {
                Card(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0XFFFFE28C)
                    )
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = Color(0XFFFF3D00),
                        modifier = Modifier.padding(all = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun ImageInterpretationScreenPreview() {
    ImageInterpretationScreen()
}

