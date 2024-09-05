package com.emirkanmaz.cookbook.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.emirkanmaz.cookbook.adapter.RecipeAdapter
import com.emirkanmaz.cookbook.databinding.FragmentListBinding
import com.emirkanmaz.cookbook.model.Recipe
import com.emirkanmaz.cookbook.roomdb.DatabaseManager
import com.emirkanmaz.cookbook.roomdb.RecipeDAO
import com.emirkanmaz.cookbook.roomdb.RecipeDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val mDisposable = CompositeDisposable()


    private lateinit var db : RecipeDatabase
    private lateinit var recipeDAO: RecipeDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DatabaseManager.getInstance(requireContext())
        recipeDAO = db.recipeDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.floatingActionButton.setOnClickListener { newCook(it) }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        getData()
    }

    private fun getData(){
        mDisposable.add(
            recipeDAO.getall()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )
    }

    private fun handleResponse(recipes: List<Recipe>){
        val adapter = RecipeAdapter(recipes)
        binding.recyclerView.adapter = adapter

    }

    private fun newCook(view: View){
        val action = ListFragmentDirections.actionListFragmentToRecipeFragment(new = true, id = 0)
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}