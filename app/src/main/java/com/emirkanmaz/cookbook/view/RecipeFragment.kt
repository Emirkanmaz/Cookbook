package com.emirkanmaz.cookbook.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.emirkanmaz.cookbook.databinding.FragmentRecipeBinding
import com.emirkanmaz.cookbook.model.Recipe
import com.emirkanmaz.cookbook.roomdb.DatabaseManager
import com.emirkanmaz.cookbook.roomdb.RecipeDAO
import com.emirkanmaz.cookbook.roomdb.RecipeDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream


class RecipeFragment : Fragment() {
    private var _binding: FragmentRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var selectedImage: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private val mDisposable = CompositeDisposable()
    private var selectedRecipe: Recipe? = null

    private lateinit var db : RecipeDatabase
    private lateinit var recipeDAO: RecipeDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = DatabaseManager.getInstance(requireContext())
        recipeDAO = db.recipeDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecipeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            it.getString("selectedImageUri")?.let { uri ->
                selectedImage = Uri.parse(uri)
            }

            it.getByteArray("selectedBitmap")?.let { bytes ->
                selectedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.imageView.setImageBitmap(selectedBitmap)
            }
        }

        binding.imageView.setOnClickListener{selectImage(it)}
        binding.saveButton.setOnClickListener {saveRecipe()}
        binding.deleteButton.setOnClickListener {deleteRecipe()}

        arguments?.let {
            val new = RecipeFragmentArgs.fromBundle(it).new

            if (new) {
                binding.deleteButton.isEnabled = false

            } else {
                val id = RecipeFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    recipeDAO.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )
                binding.saveButton.setOnClickListener { updateRecipe(id) }
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selectedImageUri", selectedImage?.toString())
        selectedBitmap?.let {
            val outputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            outState.putByteArray("selectedBitmap", outputStream.toByteArray())
        }
    }

    private fun selectImage(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                Snackbar.make(view, "Need Permission", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission") {
                        permissionLauncher.launch(permission)
                    }.show()
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImage = uri
                    try {
                        selectedBitmap = if (Build.VERSION.SDK_INT >= 29) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                        }
                        binding.imageView.setImageBitmap(selectedBitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intent)
            } else {
                Toast.makeText(requireContext(), "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createSmallBitmap(image: Bitmap, maxLength: Int): Bitmap {
        val (width, height) = if (image.width > image.height) {
            maxLength to (maxLength * image.height / image.width)
        } else {
            (maxLength * image.width / image.height) to maxLength
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun saveRecipe() {
        var name = binding.nameEditText.text.toString().trim()
        var ingredient = binding.ingredientsEditText.text.toString().trim()

        selectedBitmap?.let {
            if (name.isNotEmpty() && ingredient.isNotEmpty()) {
                val smallBitmap = createSmallBitmap(it, 300)
                val outputStream = ByteArrayOutputStream()
                smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
                val imageByteArray = outputStream.toByteArray()

                val recipe = Recipe(name = name, ingredient = ingredient, image = imageByteArray)
                mDisposable.add(
                    recipeDAO.insertAll(recipe)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponseForInsert)
                )
            }
        }
    }

    private fun updateRecipe(id: Int) {
        val name = binding.nameEditText.text.toString().trim()
        val ingredient = binding.ingredientsEditText.text.toString().trim()

        if (selectedBitmap == null) {
            selectedRecipe?.let { recipe ->
                if (name.isNotEmpty() && ingredient.isNotEmpty()) {
                    mDisposable.add(
                        recipeDAO.updateById(
                            id,
                            name,
                            ingredient,
                            recipe.image // Mevcut görüntü
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::handleResponseForInsert)
                    )
                }
            }
        } else {
            selectedBitmap?.let { bitmap ->
                if (name.isNotEmpty() && ingredient.isNotEmpty()) {
                    val smallBitmap = createSmallBitmap(bitmap, 300)
                    val outputStream = ByteArrayOutputStream()
                    smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
                    val imageByteArray = outputStream.toByteArray()

                    mDisposable.add(
                        recipeDAO.updateById(
                            id,
                            name,
                            ingredient,
                            imageByteArray
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::handleResponseForInsert)
                    )
                }
            }
        }
    }


    private fun deleteRecipe() {
        selectedRecipe?.let {
            mDisposable.add(
                recipeDAO.deleteAll(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }
    }

    private fun handleResponseForInsert(){
        val action = RecipeFragmentDirections.actionRecipeFragmentToListFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    private fun handleResponse(recipe: Recipe){
        binding.nameEditText.setText(recipe.name)
        binding.ingredientsEditText.setText(recipe.ingredient)
        val bitmap = BitmapFactory.decodeByteArray(recipe.image, 0, recipe.image.size)
        binding.imageView.setImageBitmap(bitmap)
        selectedRecipe = recipe
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}